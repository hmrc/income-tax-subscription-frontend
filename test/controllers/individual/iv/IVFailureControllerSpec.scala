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

package controllers.individual.iv

import common.Constants.ITSASessionKeys
import controllers.individual.ControllerBaseSpec
import models.audits.IVOutcomeFailureAuditing.IVOutcomeFailureAuditModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.{never, verify, when}
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, defaultAwaitTimeout, session, status}
import play.twirl.api.HtmlFormat
import services.AuditModel
import services.mocks.MockAuditingService
import views.html.individual.iv.IVFailure

import scala.concurrent.Future

class IVFailureControllerSpec extends ControllerBaseSpec with MockAuditingService {

  val controllerName: String = "IVFailureController"
  val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "failure" -> new IVFailureController(mockAuthService, mockAuditingService, mock[IVFailure]).failure()
  )

  trait Setup {
    val ivFailure: IVFailure = mock[IVFailure]
    val controller = new IVFailureController(mockAuthService, mockAuditingService, ivFailure)
  }

  authorisationTests()

  "failure" when {
    "the user has an iv flag in session" must {
      "return the failure page to the user and remove the flag from session" in new Setup {
        when(ivFailure()(any(), any())) thenReturn HtmlFormat.empty

        val requestWithIVSession: Request[AnyContent] = FakeRequest("GET", "/test-url?journeyId=testJourneyId")
          .withSession(ITSASessionKeys.IdentityVerificationFlag -> "true")
        val result: Future[Result] = controller.failure(requestWithIVSession)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
        verify(mockAuditingService).audit(matches(IVOutcomeFailureAuditModel("testJourneyId")))(any(), any())
      }
    }
    "the user does not have an iv flag in session" must {
      "return the failure page to the user" in new Setup {
        when(ivFailure()(any(), any())) thenReturn HtmlFormat.empty

        val requestWithoutIVSession: Request[AnyContent] = FakeRequest()
        val result: Future[Result] = controller.failure(requestWithoutIVSession)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
        verify(mockAuditingService, never()).audit(any[AuditModel]())(any(), any())
      }
    }
  }

}
