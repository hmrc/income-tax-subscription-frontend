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

package controllers.individual.claimenrolment

import agent.audit.mocks.MockAuditingService
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockClaimEnrolmentService
import uk.gov.hmrc.http.NotFoundException
import utilities.ITSASessionKeys
import views.individual.mocks.MockAddMTDITOverview

import scala.concurrent.Future

class AddMTDITOverviewControllerSpec extends ControllerBaseSpec
  with FeatureSwitching
  with MockClaimEnrolmentService
  with MockAuditingService
  with MockAddMTDITOverview {

  override val controllerName: String = "AddMTDITOverviewController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAddMTDITOverviewController.show,
    "submit" -> TestAddMTDITOverviewController.submit
  )

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  object TestAddMTDITOverviewController extends AddMTDITOverviewController(
    addMTDITOverview,
    mockAuditingService,
    mockAuthService
  )

  "show" should {
    "return an OK status with the add mtdit overview page and put the user into a claim enrolment journey state" when {
      "the claim enrolment feature switch is enabled" in {
        enable(ClaimEnrolment)
        mockAddMTDITOverview(postAction = routes.AddMTDITOverviewController.submit)

        val result: Future[Result] = TestAddMTDITOverviewController.show()(FakeRequest())

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
        session(result).get(ITSASessionKeys.JourneyStateKey) mustBe Some(ClaimEnrolmentJourney.name)
      }
    }
    "return a NotFoundException" when {
      "the claim enrolment feature switch is disabled" in {
        intercept[NotFoundException](await(TestAddMTDITOverviewController.show()(FakeRequest())))
          .message mustBe "[AddMTDITOverviewController][show] - The claim enrolment feature switch is disabled"
      }
    }
  }

  "submit" when {
    "the claim enrolment feature switch is disabled" should {
      "throw a NotFoundException with details that the feature switch is disabled" in {
        intercept[NotFoundException](await(TestAddMTDITOverviewController.submit()(claimEnrolmentRequest)))
          .message mustBe "[AddMTDITOverviewController][submit] - The claim enrolment feature switch is disabled"
      }
    }
    "the claim enrolment feature switch is enabled" should {
      "redirect the user to the claim enrolment resolver" in {
        enable(ClaimEnrolment)

        val result: Future[Result] = TestAddMTDITOverviewController.submit()(claimEnrolmentRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClaimEnrolmentResolverController.resolve.url)
      }
    }
  }

}
