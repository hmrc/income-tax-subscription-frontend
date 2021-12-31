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

package controllers.individual.claimenrolment

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.{ClaimEnrolment, SPSEnabled}
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import models.audits.ClaimEnrolAddToIndivCredAuditing.ClaimEnrolAddToIndivCredAuditingModel
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.claimenrolment.ClaimEnrolmentService.{AlreadySignedUp, ClaimEnrolmentError, ClaimEnrolmentSuccess, NotSubscribed}
import services.mocks.MockClaimEnrolmentService
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.agent.TestConstants

import scala.concurrent.Future

class ClaimEnrolmentResolverControllerSpec extends ControllerBaseSpec
  with FeatureSwitching
  with MockClaimEnrolmentService
  with MockAuditingService {

  override val controllerName: String = "ClaimEnrolmentResolverController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "resolve" -> TestClaimEnrolmentResolverController.resolve
  )

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    disable(SPSEnabled)
    super.beforeEach()
  }

  object TestClaimEnrolmentResolverController extends ClaimEnrolmentResolverController(
    claimEnrolmentService,
    mockAuditingService,
    mockAuthService
  )

  "resolve" when {
    "the claim enrolment feature switch is disabled" should {
      "throw a NotFoundException with details that the feature switch is disabled" in {
        intercept[NotFoundException](await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)))
          .message mustBe "[ClaimEnrolmentResolverController][submit] - The claim enrolment feature switch is disabled"
      }
    }
    "the claim enrolment feature switch is enabled" when {
      "the claim enrolment service returned a claim enrolment success and an auditing has been sent" when {
        "the SPS Enabled feature switch is enabled" should {
          "redirect the user to the SPS preference capture journey" in {
            enable(ClaimEnrolment)
            enable(SPSEnabled)
            mockClaimEnrolment(response = ClaimEnrolmentSuccess(TestConstants.testNino, "mtditid"))

            val result = await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest))

            verifyAudit(ClaimEnrolAddToIndivCredAuditingModel(TestConstants.testNino, "mtditid"))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.spsClaimEnrol.routes.SPSHandoffForClaimEnrolController.redirectToSPS.url)

          }
        }
        "the SPS Enabled feature switch is disabled" should {
          "redirect the user to the claim enrolment confirmation page" in {
            enable(ClaimEnrolment)
            mockClaimEnrolment(response = ClaimEnrolmentSuccess(TestConstants.testNino, "mtditid"))

            val result= await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest))

            verifyAudit(ClaimEnrolAddToIndivCredAuditingModel(TestConstants.testNino, "mtditid"))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.show().url)
          }
        }
      }
      "the claim enrolment service returns a not subscribed response" should {
        "redirect the user to the not subscribed page" in {
          enable(ClaimEnrolment)
          mockClaimEnrolment(response = NotSubscribed)

          val result: Future[Result] = TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.NotSubscribedController.show().url)
        }
      }
      "the claim enrolment service returns a already signed up response" should {
        "redirect the user to the already signed up page" in {
          enable(ClaimEnrolment)
          mockClaimEnrolment(response = AlreadySignedUp)

          val result: Future[Result] = TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ClaimEnrolmentAlreadySignedUpController.show.url)
        }
      }
      "the claim enrolment service returns a claim enrolment error" should {
        "throw an InternalServerException with details" in {
          enable(ClaimEnrolment)
          mockClaimEnrolment(response = ClaimEnrolmentError(msg = "claim enrolment service error"))

          intercept[InternalServerException](await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)))
            .message mustBe "claim enrolment service error"
        }
      }
    }
  }

}
