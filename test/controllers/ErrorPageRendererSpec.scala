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

package controllers

import auth.{MockAuthConnector, authenticatedFakeRequest}
import config.{AppConfig, BaseControllerConfig}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class ErrorPageRendererSpec extends ControllerBaseSpec
  with MockAuthConnector {

  // this is a trait test and does not have any auth routes to test
  override val controllerName: String = "ErrorPageRenderer"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val m: MessagesApi = messagesApi

  // test controller which makes use of the error pages
  object TestErrorPageRenderer extends BaseController with ErrorPageRenderer {
    override val baseConfig: BaseControllerConfig = MockBaseControllerConfig
    override lazy val authConnector = TestAuthConnector
    override lazy val applicationConfig: AppConfig = appConfig
    override val messagesApi: MessagesApi = m

    def displayBadRequest: Action[AnyContent] = Action.async { implicit request => showBadRequest }

    def displayNotFound: Action[AnyContent] = Action.async { implicit request => showNotFound }

    def displayInternalServerError: Action[AnyContent] = Action.async { implicit request => showInternalServerError }

  }

  "ErrorPageRenderer.displayBadRequest" should {
    lazy val result = TestErrorPageRenderer.displayBadRequest(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 400" in {
      status(result) must be(Status.BAD_REQUEST)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the bad request error page" in {
      document.title mustBe "Bad request - 400"
      document.getElementsByTag("h1").text() mustBe "Bad request"
    }
  }

  "ErrorPageRenderer.showNotFound" should {
    lazy val result = TestErrorPageRenderer.displayNotFound(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 404" in {
      status(result) must be(Status.NOT_FOUND)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the not found error page" in {
      document.title mustBe "Page not found - 404"
      document.getElementsByTag("h1").text() mustBe "This page can’t be found"
    }
  }

  "ErrorPageRenderer.displayInternalServerError" should {
    lazy val result = TestErrorPageRenderer.displayInternalServerError(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 500" in {
      status(result) must be(Status.INTERNAL_SERVER_ERROR)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the internal server error page" in {
      document.title mustBe "Sorry, we are experiencing technical difficulties - 500"
      document.getElementsByTag("h1").text() mustBe "Sorry, we’re experiencing technical difficulties"
    }
  }
}
