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

import core.audit.Logging
import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import incometax.incomesource.forms.OtherIncomeForm
import incometax.incomesource.models.OtherIncomeModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}

class OtherIncomeErrorControllerNewIncomeSourceSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "OtherIncomeErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestOtherIncomeErrorController extends OtherIncomeErrorController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.beforeEach()
    disable(NewIncomeSourceFlowFeature)
  }

  "Calling the show action of the OtherIncomeErrorController" should {

    lazy val result = TestOtherIncomeErrorController.show(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }

  "Calling the submit action of the OtherIncomeError controller with an authorised user" should {

    def callSubmit = TestOtherIncomeErrorController.submit(subscriptionRequest
      .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_no)))


    s"redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the business journey" in {

      setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(
        rentUkProperty = TestModels.testRentUkProperty_no_property,
        workForYourself = TestModels.testWorkForYourself_yes
      ))

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)

      await(goodRequest)
      verifyKeystore(fetchAll = 1)
    }

    s"redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property 1 page journey" in {

      setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(
        rentUkProperty = TestModels.testRentUkProperty_property_only,
        workForYourself = None
      ))

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.show().url)

      await(goodRequest)
      verifyKeystore(fetchAll = 1)
    }

    s"redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property 2 pages journey" in {

      setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(
        rentUkProperty = TestModels.testRentUkProperty_property_and_other,
        workForYourself = TestModels.testWorkForYourself_no
      ))

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.show().url)

      await(goodRequest)
      verifyKeystore(fetchAll = 1)
    }

    s"redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the both journey" in {

      setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(
        rentUkProperty = TestModels.testRentUkProperty_property_and_other,
        workForYourself = TestModels.testWorkForYourself_yes
      ))

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)

      await(goodRequest)
      verifyKeystore(fetchAll = 1)
    }

  }

  authorisationTests()
}

