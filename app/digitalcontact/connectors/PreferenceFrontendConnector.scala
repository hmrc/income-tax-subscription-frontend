/*
 * Copyright 2019 HM Revenue & Customs
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

package digitalcontact.connectors

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import core.Constants._
import core.audit.Logging
import core.config.{AppConfig, ITSAHeaderCarrierForPartialsConverter}
import core.utils.HttpResult._
import digitalcontact.httpparsers.PaperlessPreferenceHttpParser._
import digitalcontact.models.{PaperlessPreferenceError, PaperlessState}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future


@Singleton
class PreferenceFrontendConnector @Inject()(appConfig: AppConfig,
                                            hc: ITSAHeaderCarrierForPartialsConverter,
                                            http: HttpClient,
                                            val messagesApi: MessagesApi,
                                            logging: Logging,
                                            applicationCrypto: ApplicationCrypto
                                           ) {

  import hc._

  lazy val returnUrl: String = returnUrl(appConfig.baseUrl)

  def checkPaperlessUrl(token: String)(implicit messages: Messages): String =
    appConfig.preferencesFrontend + checkPaperlessUri(returnUrl, token)

  def choosePaperlessUrl(implicit messages: Messages): String =
    appConfig.preferencesFrontendRedirect + choosePaperlessUri(returnUrl)

  def checkPaperless(token: String)(implicit request: Request[AnyContent], messages: Messages): Future[Either[PaperlessPreferenceError.type, PaperlessState]] = {
    // The header carrier must include the current user's session in order to be authenticated by the preferences-frontend service
    // this header is converted implicitly by functions in core.config.ITSAHeaderCarrierForPartialsConverter which implements
    // uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
    http.PUT[String, HttpResult[PaperlessState]](checkPaperlessUrl(token), "") map {
      case Right(paperlessState) => Right(paperlessState)
      case Left(error) =>
        logging.warn(s"PreferencesFrontendConnector#checkPaperless failed. Returned status:${error.httpResponse.status} body:${error.httpResponse.body}")
        Left(PaperlessPreferenceError)
    }
  }

  private[digitalcontact] def urlEncode(text: String) = URLEncoder.encode(text, "UTF-8")

  private[digitalcontact] def encryptAndEncode(s: String) = urlEncode(applicationCrypto.QueryParameterCrypto.encrypt(PlainText(s)).value)

  def returnUrl(baseUrl: String): String =
    encryptAndEncode(baseUrl + digitalcontact.controllers.routes.PreferencesController.callback().url)

  private[digitalcontact] def returnLinkText(implicit messages: Messages): String = encryptAndEncode(Messages("preferences.returnLinkText"))

  def checkPaperlessUri(returnUrl: String, token: String)(implicit messages: Messages): String =
    s"""/paperless/activate-from-token/$preferencesServiceKey/$token?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""

  def choosePaperlessUri(returnUrl: String)(implicit messages: Messages): String =
    s"""/paperless/choose?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""
}