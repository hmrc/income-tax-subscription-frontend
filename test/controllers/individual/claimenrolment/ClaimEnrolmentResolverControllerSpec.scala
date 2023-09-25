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

package controllers.individual.claimenrolment

import controllers.ControllerBaseSpec
import models.audits.ClaimEnrolAddToIndivCredAuditing.ClaimEnrolAddToIndivCredAuditingModel
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.claimenrolment.ClaimEnrolmentService.{AlreadySignedUp, ClaimEnrolmentError, ClaimEnrolmentSuccess, NotSubscribed}
import services.mocks.{MockAuditingService, MockClaimEnrolmentService}
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.TestConstants

import scala.concurrent.Future

class ClaimEnrolmentResolverControllerSpec extends ControllerBaseSpec

  with MockClaimEnrolmentService
  with MockAuditingService {

  override val controllerName: String = "ClaimEnrolmentResolverController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "resolve" -> TestClaimEnrolmentResolverController.resolve
  )

  object TestClaimEnrolmentResolverController extends ClaimEnrolmentResolverController(
    claimEnrolmentService,
    mockAuditingService,
    mockAuthService
  )


  "the claim enrolment service returned a claim enrolment success and an auditing has been sent" should {
    "redirect the user to the SPS preference capture journey" in {
      mockClaimEnrolment(response = Right(ClaimEnrolmentSuccess(TestConstants.testNino, "mtditid")))

      val result = await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest))

      verifyAudit(ClaimEnrolAddToIndivCredAuditingModel(TestConstants.testNino, "mtditid"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.spsClaimEnrol.routes.SPSHandoffForClaimEnrolController.redirectToSPS.url)
    }
  }

  "the claim enrolment service returns a not subscribed response" should {
    "throw an InternalServerException with details" in {

      mockClaimEnrolment(response = Left(ClaimEnrolmentError(msg = "User was not subscribed")))

      intercept[InternalServerException](await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)))
        .message mustBe "User was not subscribed"
    }
  }
  "the claim enrolment service returns a already signed up response" should {
    "redirect the user to the already signed up page" in {
      mockClaimEnrolment(response = Left(AlreadySignedUp))

      val result: Future[Result] = TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ClaimEnrolmentAlreadySignedUpController.show.url)
    }
  }

  "the claim enrolment service returns a claim enrolment error" should {
    "throw an InternalServerException with details" in {

      mockClaimEnrolment(response = Left(ClaimEnrolmentError(msg = "claim enrolment service error")))

      intercept[InternalServerException](await(TestClaimEnrolmentResolverController.resolve()(claimEnrolmentRequest)))
        .message mustBe "claim enrolment service error"
    }
  }

}
