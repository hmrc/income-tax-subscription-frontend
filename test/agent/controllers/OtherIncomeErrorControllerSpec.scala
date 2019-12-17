/*
 * Copyright 2019 HM Revenue & Customs
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
import agent.forms.OtherIncomeForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.models.No
import incometax.subscription.models.Property
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class OtherIncomeErrorControllerSpec extends AgentControllerBaseSpec with MockKeystoreService with FeatureSwitching {

  override val controllerName: String = "OtherIncomeErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestOtherIncomeErrorController extends OtherIncomeErrorController()(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentPropertyCashOrAccruals)
  }

  "Calling the showOtherIncomeError action of the OtherIncomeErrorController" should {

    lazy val result = TestOtherIncomeErrorController.show(FakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }

  "Calling the submitOtherIncomeError action of the OtherIncomeError controller with an authorised user" should {

    def callSubmit: Future[Result] = TestOtherIncomeErrorController.submit(subscriptionRequest
      .post(OtherIncomeForm.otherIncomeForm, No))

    s"redirect to '${agent.controllers.business.routes.MatchTaxYearController.show().url}' on the business journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.MatchTaxYearController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

    s"redirect to ${business.routes.PropertyAccountingMethodController.show().url}" when {
      "the user is on a property only journey and the property cash/accruals feature switch is enabled" in {
        enable(AgentPropertyCashOrAccruals)

        setupMockKeystore(fetchIncomeSource = Property)

        val goodRequest = await(callSubmit)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(business.routes.PropertyAccountingMethodController.show().url)

        verifyKeystore(fetchIncomeSource = 1)
      }
    }

    s"redirect to '${agent.controllers.routes.TermsController.show().url}' on the property journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

    s"redirect to '${agent.controllers.routes.CheckYourAnswersController.show().url}' on the property journey when the eligibility feature switch os enabled" in {
      enable(EligibilityPagesFeature)
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
      disable(EligibilityPagesFeature)
    }

    s"redirect to '${agent.controllers.business.routes.MatchTaxYearController.show().url}' on the both journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.MatchTaxYearController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

  }

  authorisationTests()
}

