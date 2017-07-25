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

package connectors

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.GGAuthenticationConnector._
import connectors.models.authenticator._
import play.api.http.Status._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class GGAuthenticationConnector @Inject()(appConfig: AppConfig,
                                          httpPost: HttpPost,
                                          logging: Logging
                                         ) extends RawResponseReads {

  lazy val refreshProfileUrl = appConfig.ggAuthenticationURL + refreshProfileUri

  def refreshProfile()(implicit hc: HeaderCarrier): Future[Either[RefreshProfileFailure.type, RefreshProfileSuccess.type]] =
    httpPost.POSTEmpty[HttpResponse](refreshProfileUrl).map {
      response =>
        response.status match {
          case NO_CONTENT =>
            logging.info(s"GGAuthentication refreshProfile responded with NO_CONTENT")
            Right(RefreshProfileSuccess)
          case status =>
            logging.warn(s"GGAuthentication refreshProfile responded with a error: status=$status body=${response.body}")
            Left(RefreshProfileFailure)
        }
    }

}

object GGAuthenticationConnector {

  val refreshProfileUri = "/government-gateway-authentication/refresh-profile"

}
