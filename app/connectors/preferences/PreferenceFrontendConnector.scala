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

package connectors.preferences

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import config.ITSAHeaderCarrierForPartialsConverter._
import connectors.RawResponseReads
import connectors.httpparsers.PaperlessPreferenceHttpParser._
import connectors.models.preferences.{PaperlessPreferenceError, PaperlessState}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.{HttpGet, HttpPut}
import utils.HttpResult._
import common.Constants._
import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._


@Singleton
class PreferenceFrontendConnector @Inject()(appConfig: AppConfig,
                                            httpGet: HttpGet,
                                            httpPut: HttpPut,
                                            val messagesApi: MessagesApi,
                                            logging: Logging
                                           ) extends I18nSupport with RawResponseReads {

  lazy val returnUrl: String = PreferenceFrontendConnector.returnUrl(appConfig.baseUrl)

  def checkPaperlessUrl(token: String): String = if(appConfig.newPreferencesApiEnabled) {
    appConfig.preferencesFrontend + PreferenceFrontendConnector.newCheckPaperlessUri(returnUrl, token)
  } else {
    appConfig.preferencesFrontend + PreferenceFrontendConnector.checkPaperlessUri(returnUrl)
  }

  def choosePaperlessUrl(implicit request: Request[AnyContent]): String =
    appConfig.preferencesFrontend + PreferenceFrontendConnector.choosePaperlessUri(returnUrl)

  def checkPaperless(token: String)(implicit request: Request[AnyContent]): Future[Either[PaperlessPreferenceError.type, PaperlessState]] = {
    // The header carrier must include the current user's session in order to be authenticated by the preferences-frontend service
    // this header is converted implicitly by functions in config.ITSAHeaderCarrierForPartialsConverter which implements
    // uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
    httpPut.PUT[String, HttpResult[PaperlessState]](checkPaperlessUrl(token), "") map {
      case Right(paperlessState) => Right(paperlessState)
      case Left(error) =>
        logging.warn(s"PreferencesFrontendConnector#checkPaperless failed. Returned status:${error.httpResponse.status} body:${error.httpResponse.body}")
        Left(PaperlessPreferenceError)
    }
  }
}

object PreferenceFrontendConnector {

  private[preferences] def urlEncode(text: String) = URLEncoder.encode(text, "UTF-8")

  private[preferences] def encryptAndEncode(s: String) = urlEncode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(s)).value)

  def returnUrl(baseUrl: String): String =
    encryptAndEncode(baseUrl + controllers.preferences.routes.PreferencesController.callback().url)

  private[preferences] def returnLinkText(implicit messages: Messages): String = encryptAndEncode(Messages("preferences.returnLinkText"))

  def checkPaperlessUri(returnUrl: String)(implicit messages: Messages): String =
    s"""/paperless/activate?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""

  def newCheckPaperlessUri(returnUrl: String, token: String)(implicit messages: Messages): String =
    s"""/paperless/activate/$preferencesServiceKey/$token?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""

  def choosePaperlessUri(returnUrl: String)(implicit messages: Messages): String =
    s"""/paperless/choose?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""

}