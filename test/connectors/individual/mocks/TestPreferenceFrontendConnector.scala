/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors.individual.mocks

import connectors.PreferenceFrontendConnector
import core.audit.Logging
import core.config.{AppConfig, ITSAHeaderCarrierForPartialsConverter}
import core.connectors.mocks.MockHttp
import core.utils.TestConstants._
import core.utils.{MockTrait, UnitTestTrait}
import models.{Activated, PaperlessPreferenceError, PaperlessState, Unset}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.ApplicationCrypto

import scala.concurrent.Future

trait MockPreferenceFrontendConnector extends MockTrait with I18nSupport {

  val mockPreferenceFrontendConnector: PreferenceFrontendConnector = mock[PreferenceFrontendConnector]
  implicit val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  private def mockCheckPaperless(token: String)(result: Future[Either[PaperlessPreferenceError.type, PaperlessState]]): Unit =
    when(mockPreferenceFrontendConnector.checkPaperless(ArgumentMatchers.eq(token))(ArgumentMatchers.any[Request[AnyContent]], ArgumentMatchers.any[Messages]))
      .thenReturn(result)

  def mockCheckPaperlessActivated(token: String): Unit = mockCheckPaperless(token)(Future.successful(Right(Activated)))

  def mockCheckPaperlessUnset(token: String): Unit = mockCheckPaperless(token)(Future.successful(Right(Unset(testUrl))))

  def mockCheckPaperlessException(token: String): Unit = mockCheckPaperless(token)(Future.failed(testException))

  def mockChoosePaperlessUrl(url: String): Unit =
    when(mockPreferenceFrontendConnector.choosePaperlessUrl(ArgumentMatchers.any())) thenReturn url

}

trait TestPreferenceFrontendConnector extends UnitTestTrait with I18nSupport with MockHttp {

  implicit val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  object TestPreferenceFrontendConnector extends PreferenceFrontendConnector(
    app.injector.instanceOf[AppConfig],
    app.injector.instanceOf[ITSAHeaderCarrierForPartialsConverter],
    mockHttp,
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[Logging],
    app.injector.instanceOf[ApplicationCrypto]
  )

  def setupCheckPaperless(token: String)(tuple: (Int, Option[JsValue]))(implicit request: Request[AnyContent]): Unit =
    setupMockCheckPaperless(token)(tuple._1, tuple._2)

  def setupMockCheckPaperless(token: String)(status: Int, response: Option[JsValue])(implicit request: Request[AnyContent]): Unit =
    setupMockHttpPut[String](url = TestPreferenceFrontendConnector.checkPaperlessUrl(token), "")(status, response)


  private def okResponseJson(paperless: Boolean): JsValue = Json.obj(
    "optedIn" -> paperless
  )

  val paperlessActivated: (Int, Option[JsValue]) = (OK, okResponseJson(true))

  val paperlessDeclined: (Int, Option[JsValue]) = (OK, okResponseJson(false))

  val paperlessPreconditionFailed: ((Int, Option[JsValue])) = (PRECONDITION_FAILED, None)

}
