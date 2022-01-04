/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.individual.subscription

import agent.audit.mocks.MockAuditingService
import controllers.{ControllerBaseSpec, FeedbackController}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.TestConstants._
import views.html.individual.incometax.subscription.enrolled.ClaimSubscription
import views.html.{Feedback, FeedbackThankyou}

class ClaimSubscriptionControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockSubscriptionOrchestrationService with MockAuditingService {

  override val controllerName: String = "ClaimSubscriptionController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "claim" -> TestClaimSubscriptionController.claim()
  )

  object TestClaimSubscriptionController extends ClaimSubscriptionController(
    mock[ClaimSubscription],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockSubscriptionOrchestrationService
  )

  "Calling the claim action of the ClaimSubscriptionController with a subscribed Authenticated User" should {
    "return a a redirect to the confirmation page" in withController { controller =>
      mockFetchSubscriptionIdFromSubscriptionDetails(testMTDID)
      mockEnrolAndRefreshSuccess(testMTDID, testNino)

      lazy val result = controller.claim(subscriptionRequest)

      status(result) must be(Status.OK)
    }

    "return an error where enrolment fails" in withController { controller =>
      mockFetchSubscriptionIdFromSubscriptionDetails(testMTDID)
      mockEnrolFailure(testMTDID, testNino)

      lazy val result = controller.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }

    "return an error where refresh profile fails" in withController { controller =>
      mockFetchSubscriptionIdFromSubscriptionDetails(testMTDID)
      mockRefreshFailure(testMTDID, testNino)

      lazy val result = controller.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }

    "return an error where Subscription Details  does not contain the MtditId" in withController { controller =>
      mockFetchSubscriptionIdFromSubscriptionDetails(fetchSubscriptionId = None)

      lazy val result = controller.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }
  }

  authorisationTests()

  private def withController(testCode: ClaimSubscriptionController => Any): Unit = {
    val claimSubscriptionView = mock[ClaimSubscription]

    when(claimSubscriptionView()(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new ClaimSubscriptionController(
      claimSubscriptionView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockSubscriptionOrchestrationService
    )

    testCode(controller)
  }
}
