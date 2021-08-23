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
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec

import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers._

import uk.gov.hmrc.http.NotFoundException
import views.individual.mocks.{MockAlreadySignedUp, MockNotSubscribed}

import scala.concurrent.Future

class AlreadySignedUpControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with FeatureSwitching
  with MockAlreadySignedUp {

  override val controllerName: String = "ClaimEnrolmentAlreadySignedUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAlreadySignedUpController.show
  )

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  object TestAlreadySignedUpController extends ClaimEnrolmentAlreadySignedUpController(
    mockAuthService,
    mockAuditingService,
    alreadySignedUp
  )

  "show" should {
    "return an OK status with the already signed up page" when {
      "the claim enrolment feature switch is enabled" in {
        enable(ClaimEnrolment)
        mockAlreadySignedUp()
        val result: Future[Result] = TestAlreadySignedUpController.show()(claimEnrolmentRequest)
        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

    "return a NotFoundException" when {
      "the claim enrolment feature switch is disabled" in {
        intercept[NotFoundException](await(TestAlreadySignedUpController.show()(claimEnrolmentRequest)))
          .message mustBe "[ClaimEnrolmentAlreadySignedUpController][show] - The claim enrolment feature switch is disabled"
      }
    }
  }

}
