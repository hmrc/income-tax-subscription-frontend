/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks._
import views.html.individual.eligibility.YouCanSignUp
import scala.concurrent.Future

class YouCanSignUpControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService
  with MockSessionDataService {

  val mockYouCanSignUp: YouCanSignUp = mock[YouCanSignUp]

  object TestYouCanSignUpController extends YouCanSignUpController(
    mockYouCanSignUp)(
    mockAuditingService,
    appConfig,
    mockAuthService
  )

  trait Setup {
    val controller: YouCanSignUpController = new YouCanSignUpController(
      mockYouCanSignUp)(
      mockAuditingService,
      appConfig,
      mockAuthService
    )
  }

  override val controllerName: String = "YouCanSignUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestYouCanSignUpController.show
  )

  "show" must {
    "return OK and render the YouCanSignUp view" in new Setup {
      when(mockYouCanSignUp(any())(any(), any()))
        .thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }
}
