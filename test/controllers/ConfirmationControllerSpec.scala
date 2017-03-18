/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import audit.Logging
import auth.{authenticatedFakeRequest, mockEnrolled}
import org.scalatest.Matchers._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds

class ConfirmationControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  object TestConfirmationController extends ConfirmationController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging]
  )

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationController.showConfirmation
  )

  "ConfirmationController" should {
    "If the user is enrolled then get the ID from keystore" in {
      setupMockKeystore(fetchSubscriptionId = "testId")
      val result = TestConfirmationController.showConfirmation(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockEnrolled))
      status(result) shouldBe OK

      await(result)
      verifyKeystore(fetchSubscriptionId = 1)
    }

    "If the user is not enrolled then return not found" in {
      setupMockKeystore(fetchSubscriptionId = "testId")
      val result = TestConfirmationController.showConfirmation(authenticatedFakeRequest())
      status(result) shouldBe NOT_FOUND

      await(result)
      verifyKeystore(fetchSubscriptionId = 0)
    }
  }

  authorisationTests()

}
