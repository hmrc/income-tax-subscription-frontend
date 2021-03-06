/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import connectors.PaperlessPreferenceHttpParser._
import models.{PaperlessPreferenceError, PaperlessState}
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utilities.HttpResult._
import utilities.individual.Constants._

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreferenceFrontendConnector @Inject()(val messagesApi: MessagesApi,
                                            appConfig: AppConfig,
                                            applicationCrypto: ApplicationCrypto,
                                            http: HttpClient,
                                            headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter)
                                           (implicit ec: ExecutionContext) {


  lazy val returnUrl: String = returnUrl(appConfig.baseUrl)

  def checkPaperlessUrl(token: String)(implicit messages: Messages): String =
    appConfig.preferencesFrontend + checkPaperlessUri(returnUrl, token)

  def choosePaperlessUrl(implicit messages: Messages): String =
    appConfig.preferencesFrontendRedirect + choosePaperlessUri(returnUrl)

  def checkPaperless(token: String)(implicit request: Request[AnyContent], messages: Messages):
  Future[Either[PaperlessPreferenceError.type, PaperlessState]] = {
    // The header carrier must include the current user's session in order to be authenticated by the preferences-frontend service
    // this header is converted implicitly by functions in config.ITSAHeaderCarrierForPartialsConverter which implements
    // uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    http.PUT[String, HttpResult[PaperlessState]](checkPaperlessUrl(token), "") map {
      case Right(paperlessState) => Right(paperlessState)
      case Left(error) =>
        Logger.warn(s"PreferencesFrontendConnector#checkPaperless failed. Returned status:${error.httpResponse.status} body:${error.httpResponse.body}")
        Left(PaperlessPreferenceError)
    }
  }

  private def urlEncode(text: String) = URLEncoder.encode(text, "UTF-8")

  private def encryptAndEncode(s: String) = urlEncode(applicationCrypto.QueryParameterCrypto.encrypt(PlainText(s)).value)

  def returnUrl(baseUrl: String): String =
    encryptAndEncode(baseUrl + controllers.individual.routes.PreferencesController.callback().url)

  private def returnLinkText(implicit messages: Messages): String = encryptAndEncode(Messages("preferences.returnLinkText"))

  def checkPaperlessUri(returnUrl: String, token: String)(implicit messages: Messages): String =
    s"""/paperless/activate-from-token/$preferencesServiceKey/$token?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""

  def choosePaperlessUri(returnUrl: String)(implicit messages: Messages): String =
    s"""/paperless/choose?returnUrl=$returnUrl&returnLinkText=$returnLinkText"""
}
