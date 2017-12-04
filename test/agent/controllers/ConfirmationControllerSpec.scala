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

package agent.controllers

import agent.audit.Logging
import agent.services.mocks.MockKeystoreService
import org.scalatest.Matchers._
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class ConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService {

  object TestConfirmationController extends ConfirmationController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationController.showConfirmation
  )

  "ConfirmationController" when {

    "submitted is not in session" should {
      "return a NotFoundException" in {
        val result = TestConfirmationController.showConfirmation(subscriptionRequest)

        intercept[NotFoundException](await(result))
      }
    }

    "submitted is in session" should {
      "Get the ID from keystore" in {
        val result = TestConfirmationController.showConfirmation(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any"))
        status(result) shouldBe OK

        await(result)
      }
    }
  }

  authorisationTests()

}
