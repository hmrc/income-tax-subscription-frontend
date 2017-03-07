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

package connectors.throttling

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.RawResponseReads
import connectors.models.throttling.{CanAccess, LimitReached, UserAccess}
import play.api.http.Status.{OK, TOO_MANY_REQUESTS}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ThrottlingControlConnector @Inject()(val appConfig: AppConfig,
                                           val http: HttpGet,
                                           logging: Logging) extends RawResponseReads {

  lazy val throttleControlUrl = (nino: String) => s"${appConfig.throttleControlUrl}/$nino"

  def checkAccess(nino: String)(implicit hc: HeaderCarrier): Future[Option[UserAccess]] = {
    http.GET[HttpResponse](throttleControlUrl(nino)).map {
      response =>
        response.status match {
          case OK => CanAccess
          case TOO_MANY_REQUESTS =>
            logging.info("ThrottlingControlConnector.checkAccess: TOO_MANY_REQUESTS")
            LimitReached
          case x =>
            logging.warn(s"ThrottlingControlConnector.checkAccess: unexpected status=$x")
            None
        }
    }
  }

}
