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

package controllers.individual.iv

import agent.audit.mocks.MockAuditingService
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import common.Constants.ITSASessionKeys
import controllers.ControllerBaseSpec
import models.audits.IVOutcomeSuccessAuditing.IVOutcomeSuccessAuditModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.{never, verify}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, session, status}
import services.AuditModel
import utilities.individual.TestConstants.testNino

import scala.concurrent.Future

class IVSuccessControllerSpec extends ControllerBaseSpec with MockAuditingService {

  val controllerName: String = "IVSuccessController"
  val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "success" -> new IVSuccessController(appConfig, mockAuthService, mockAuditingService).success()
  )


  trait Setup {
    val controller = new IVSuccessController(appConfig, mockAuthService, mockAuditingService)
  }

  authorisationTests()

  "success" when {

          "the user has an iv flag in session" when {
            "the is in claimEnrollment journey state" must {
              "redirect the user to the claim enrolment overview page and remove the iv flag from session" in new Setup {

                val requestWithIVSession: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.IdentityVerificationFlag -> "true", ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name)
                val result: Future[Result] = controller.success(requestWithIVSession)

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentResolverController.resolve.url)
                session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
                verify(mockAuditingService).audit(matches(IVOutcomeSuccessAuditModel(testNino)))(any(), any())
              }
            }
          }
        }

        "the user has an iv flag in session" must {
          "redirect the user to the home route and remove the iv flag from session" in new Setup {

            val requestWithIVSession: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.IdentityVerificationFlag -> "true")
            val result: Future[Result] = controller.success(requestWithIVSession)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.usermatching.routes.HomeController.home.url)
            session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
            verify(mockAuditingService).audit(matches(IVOutcomeSuccessAuditModel(testNino)))(any(), any())
          }
        }
        "the user doesn't not have an iv flag in session" must {
          "redirect the user to the home route" in new Setup {

            val requestWithoutIVSession: Request[AnyContent] = FakeRequest()
            val result: Future[Result] = controller.success(requestWithoutIVSession)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.usermatching.routes.HomeController.home.url)
            session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
            verify(mockAuditingService, never()).audit(any[AuditModel]())(any(), any())
          }
        }
      }
