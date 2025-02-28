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

import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import common.Constants.ITSASessionKeys
import controllers.individual.ControllerBaseSpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, session, status}
import services.mocks.{MockAuditingService, MockNinoService}
import utilities.individual.TestConstants.testNino

import scala.concurrent.Future

class IVSuccessControllerSpec extends ControllerBaseSpec with MockAuditingService with MockNinoService {

  val controllerName: String = "IVSuccessController"
  val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "success" -> new IVSuccessController(appConfig, mockAuthService, mockNinoService, mockAuditingService).success()
  )

  trait Setup {
    val controller = new IVSuccessController(appConfig, mockAuthService, mockNinoService, mockAuditingService)
  }

  authorisationTests()

  "success" when {

    "the user has an iv flag in session" when {
      "the is in claimEnrollment journey state" must {
        "redirect the user to the claim enrolment overview page and remove the iv flag from session" in new Setup {
          mockGetNino(testNino)

          val requestWithIVSession: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.IdentityVerificationFlag -> "true", ITSASessionKeys.JourneyStateKey -> ClaimEnrolmentJourney.name)
          val result: Future[Result] = controller.success(requestWithIVSession)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentResolverController.resolve.url)
          session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
        }
      }
    }
  }

  "the user has an iv flag in session" must {
    "redirect the user to the index route and remove the iv flag from session" in new Setup {
      mockGetNino(testNino)

      val requestWithIVSession: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.IdentityVerificationFlag -> "true")
      val result: Future[Result] = controller.success(requestWithIVSession)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.matching.routes.HomeController.index.url)
      session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
    }
  }
  "the user doesn't not have an iv flag in session" must {
    "redirect the user to the index route" in new Setup {
      val requestWithoutIVSession: Request[AnyContent] = FakeRequest()
      val result: Future[Result] = controller.success(requestWithoutIVSession)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.matching.routes.HomeController.index.url)
      session(result).get(ITSASessionKeys.IdentityVerificationFlag) mustBe None
    }
  }
}
