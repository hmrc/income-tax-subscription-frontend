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

package controllers

import auth.MockHttp
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status.OK
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever, HeaderCarrierForPartialsConverter}
import views.html.{Feedback, FeedbackThankyou}

import scala.concurrent.Future

class FeedbackControllerSpec extends ControllerBaseSpec with MockHttp {

  override val controllerName: String = "FeedbackController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  implicit lazy val mockHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter = app.injector.instanceOf[HeaderCarrierForPartialsConverter]

  implicit lazy val mockFormPartialRetriever: FormPartialRetriever = app.injector.instanceOf[FormPartialRetriever]

  implicit lazy val mockCachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = app.injector.instanceOf[CachedStaticHtmlPartialRetriever]

  "show" when {
    "referrer is in the request headers but not the session" must withController { controller =>
      s"return $OK and add the referrer to session" in {
        val request: Request[AnyContent] = FakeRequest().withHeaders(HeaderNames.REFERER -> "test-referer")
        val result: Future[Result] = controller.show()(request)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        session(result).get(REFERER) mustBe Some("test-referer")
      }
    }
    "referrer is not in the headers" must withController { controller =>
      s"return $OK" in {
        val request: Request[AnyContent] = FakeRequest().withSession(HeaderNames.REFERER -> "test-referer")
        val result: Future[Result] = controller.show()(request)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }
  }

  "submit" must {
    s"redirect to ${controllers.routes.FeedbackController.thankyou.url}" when {
      "the form was successfully submitted" in withController { controller =>
        val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody("testKey" -> "testData")

        when(mockHttp.POSTForm[HttpResponse](
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(Map("testKey" -> Seq("testData"))),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )).thenReturn(Future.successful(HttpResponse(OK, body = "test-ticket-id")))

        val result: Future[Result] = controller.submit()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.FeedbackController.thankyou.url)
        session(result).get("ticketId") mustBe Some("test-ticket-id")
      }
    }
    s"return a bad request with a page" when {
      "a bad request is returned" in withController { controller =>
        val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody("testKey" -> "testData")

        when(mockHttp.POSTForm[HttpResponse](
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(Map("testKey" -> Seq("testData"))),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, body = "test-response")))

        val result: Future[Result] = controller.submit()(request)

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some("text/html")
        session(result).get("ticketId") mustBe None
      }
    }
    s"return an internal server error" when {
      "any other status is returned from the post" in withController { controller =>
        val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody("testKey" -> "testData")

        when(mockHttp.POSTForm[HttpResponse](
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(Map("testKey" -> Seq("testData"))),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val result: Future[Result] = controller.submit()(request)

        intercept[InternalServerException](await(result)).message mustBe "feedback controller, submit call failed"
      }
      "the form submitted is empty" in withController { controller =>
        val request: Request[AnyContent] = FakeRequest()

        val result: Future[Result] = controller.submit(request)

        intercept[InternalServerException](await(result)).message mustBe "feedback controller, tried to submit empty feedback form"
      }
    }
  }

  private def withController(testCode: FeedbackController => Any): Unit = {
    val feedbackView = mock[Feedback]

    when(feedbackView(any(), any())(any(), any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val feedbackThankYouView = mock[FeedbackThankyou]

    when(feedbackThankYouView(any(), any())(any(), any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new FeedbackController(
      feedbackView,
      feedbackThankYouView,
      mockHttp
    )(
      mockFormPartialRetriever,
      appConfig,
      executionContext,
      mockMessagesControllerComponents,
      mockHeaderCarrierForPartialsConverter
    )

    testCode(controller)
  }
}
