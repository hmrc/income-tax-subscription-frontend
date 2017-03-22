/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config.filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import config.AppConfig
import play.api.{Application, Environment}
import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{Call, RequestHeader, Result}
import uk.gov.hmrc.play.config.inject.RunMode
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

import scala.concurrent.Future

@Singleton
class WhitelistFilter @Inject()(app: Application
                               ) extends AkamaiWhitelistFilter with MicroserviceFilterSupport with RunMode {
  override val environment: Environment = app.injector.instanceOf[Environment]

  // START of crazy section
  // this code is copied exactly as they are from AkamaiWhitelistFilter,
  // for some reason if we do not do this then it cannot find the play.api.mvc.Call method
  private def isCircularDestination(requestHeader: RequestHeader): Boolean =
  requestHeader.uri == destination.url

  private def toCall(rh: RequestHeader): Call =
    Call(rh.method, rh.uri)

  override def apply
  (f: (RequestHeader) => Future[Result])
  (rh: RequestHeader): Future[Result] =
    if (excludedPaths contains toCall(rh)) {
      f(rh)
    } else {
      rh.headers.get(trueClient) map {
        ip =>
          if (whitelist.contains(ip))
            f(rh)
          else if (isCircularDestination(rh))
            Future.successful(Forbidden)
          else
            Future.successful(Redirect(destination))
      } getOrElse Future.successful(Redirect(destination))
    }

  // END of crazy section

  private lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override implicit lazy val mat: Materializer = app.injector.instanceOf[Materializer]

  override lazy val whitelist: Seq[String] = appConfig.whitelistIps
  override lazy val destination: Call = Call("GET", appConfig.shutterPage)
  override lazy val excludedPaths: Seq[Call] = appConfig.ipExclusionList
}

