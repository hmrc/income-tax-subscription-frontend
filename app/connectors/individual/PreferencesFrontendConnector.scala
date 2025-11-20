/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.individual

import config.AppConfig
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreferencesFrontendConnector @Inject()(appConfig: AppConfig, http: HttpClientV2, crypto: ApplicationCrypto)
                                            (implicit ec: ExecutionContext) extends HeaderCarrierForPartialsConverter with Logging {

  val url: String = activateUrl(
    returnUrl = encryptAndEncodeString("/"),
    returnLinkText = encryptAndEncodeString("")
  )

  def getOptedInStatus(implicit request: Request[AnyContent]): Future[Option[Boolean]] = {
    http
      .put(url"$url")
      .withBody(Json.obj())
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => (response.json \ "optedIn").validate[Boolean] match {
            case JsSuccess(optedIn, _) =>
              Some(optedIn)
            case _ =>
              logger.error("[PreferencesFrontendConnector][getOptedInStatus] - Could not retrieve optedIn value")
              None
          }
          case status =>
            logger.warn(s"[PreferencesFrontendConnector][getOptedInStatus] - Unexpected status returned - $status")
            None
        }

      }
  }

  private def activateUrl(returnUrl: String, returnLinkText: String): String = {
    s"${appConfig.preferencesFrontend}/paperless/activate?returnUrl=$returnUrl&returnLinkText=$returnLinkText"
  }

  private def encryptAndEncodeString(value: String): String = {
    URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")
  }

}
