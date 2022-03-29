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

package controllers.individual.claimenrolment.spsClaimEnrol

import agent.audit.mocks.MockAuditingService
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import controllers.ControllerBaseSpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import services.individual.claimenrolment.ClaimEnrolmentService.ClaimEnrolmentError
import services.mocks.{MockClaimEnrolmentService, MockSpsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.ITSASessionKeys
import utilities.individual.TestConstants

import scala.concurrent.Future

class SPSCallbackForClaimEnrolControllerSpec extends ControllerBaseSpec with MockAuditingService with MockSpsService
  with MockClaimEnrolmentService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ClaimEnrolment)
  }

  object TestSPSCallbackForClaimEnrolController extends SPSCallbackForClaimEnrolController(
    mockAuditingService,
    mockAuthService,
    mockSpsService,
    claimEnrolmentService
  )

  override val controllerName: String = "PreferencesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "callback" -> TestSPSCallbackForClaimEnrolController.callback
  )

  def request(hasEntityId: Boolean): Request[_] = {
    val entityIdParam: String = if (hasEntityId) {
      "?entityId=testId"
    } else {
      ""
    }
    FakeRequest("GET", controllers.individual.claimenrolment.spsClaimEnrol.routes.SPSCallbackForClaimEnrolController.callback.url + entityIdParam).withSession(
      ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name,
      ITSASessionKeys.NINO -> TestConstants.testNino,
      ITSASessionKeys.UTR -> TestConstants.testUtr
    )
  }

  "callback" when {
    "the claim enrolment feature switch set to false" should {
      "return a not found exception" in {
        val result: Future[Result] = TestSPSCallbackForClaimEnrolController.callback(request(hasEntityId = true)).run()

        intercept[NotFoundException](await(result)).message mustBe "[SPSCallbackForClaimEnrolController][callback] - claim enrolment feature switch is not enabled"
      }
    }

    "the claim enrolment feature switch is enabled" when {
      "an entityId is passed through to the url" when {
        "mtditid successfully retrieves from claimEnrolmentService" should {
          "link preference with mtditid to sps, save the entityId in session and redirect to the claim enrolment confirmation page" in {
            enable(ClaimEnrolment)
            mockGetMtditidFromSubscription("mtditid")

            val result: Future[Result] = TestSPSCallbackForClaimEnrolController.callback(request(hasEntityId = true)).run()

            verifyConfirmPreferencesPostSpsConfirm("mtditid", "testId", 1)
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.show().url)
          }
        }
        "mtditid failed to retrieve from claimEnrolmentService" should {
          "throw an InternalServerException" in {
            enable(ClaimEnrolment)
            mockGetMtditidFromSubscription(ClaimEnrolmentError(msg = "failed to retrieve mtditid from claimEnrolmentService"))

            val result = TestSPSCallbackForClaimEnrolController.callback(request(hasEntityId = true)).run()

            intercept[InternalServerException](await(result))
              .message mustBe "[SPSCallbackForClaimEnrolController][callback] - failed to retrieve mtditid from claimEnrolmentService"
          }
        }
      }
      "no entityId is present in the url" should {
        "redirect the user to the claim enrolment confirmation page " in {
          enable(ClaimEnrolment)

          val result: Future[Result] = TestSPSCallbackForClaimEnrolController.callback(request(hasEntityId = false)).run()

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.show().url)
        }
      }
    }
  }

  authorisationTests()

}
