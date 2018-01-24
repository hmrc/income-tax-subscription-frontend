/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.controllers

import assets.MessageLookup.{CannotSignUp => messages}
import core.audit.Logging
import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class CannotSignUpControllerSpec extends ControllerBaseSpec with MockKeystoreService with FeatureSwitching {

  override val controllerName: String = "CannotSignUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(NewIncomeSourceFlowFeature)
  }

  object TestCannotSignUpController extends CannotSignUpController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

  "Calling the show action of the CannotSignUpController" when {
    def call = TestCannotSignUpController.show(subscriptionRequest)

    "the new income source flow feature is disabled" should {
      "return not found (404)" in {
        disable(NewIncomeSourceFlowFeature)
        intercept[NotFoundException](await(call))
      }
    }

    "the new income source flow feature is enabled" should {
      "return ok (200)" in {
        val result = call
        val document = Jsoup.parse(contentAsString(result))
        status(result) must be(Status.OK)
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
        document.title mustBe messages.title
      }
    }
  }

}
