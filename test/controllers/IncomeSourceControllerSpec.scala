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

import auth._
import forms.IncomeSourceForm
import models.IncomeSourceModel
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class IncomeSourceControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showIncomeSource" -> TestIncomeSourceController.showIncomeSource(isEditMode = true),
    "submitIncomeSource" -> TestIncomeSourceController.submitIncomeSource(isEditMode = true)
  )

  object TestIncomeSourceController extends IncomeSourceController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(authenticatedFakeRequest())
      m must not be null
      m.apply("base.back") must be("Back")
    }
  }

  "Calling the showIncomeSource action of the IncomeSource controller with an authorised user" should {

    lazy val result = TestIncomeSourceController.showIncomeSource(isEditMode = true)(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeSource = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and valid submission" should {

    def callShow(option: String, isEditMode: Boolean) = TestIncomeSourceController.submitIncomeSource(isEditMode = isEditMode)(authenticatedFakeRequest()
      .post(IncomeSourceForm.incomeSourceForm, IncomeSourceModel(option)))

    "When it is not edit mode" should {
        s"return an SEE OTHER (303) for business and goto ${controllers.business.routes.SoleTraderController.showSoleTrader().url}" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(IncomeSourceForm.option_business, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.business.routes.SoleTraderController.showSoleTrader().url

          await(goodRequest)
          verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
        }

        s"return a SEE OTHER (303) for property and goto ${controllers.property.routes.PropertyIncomeController.showPropertyIncome().url}" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(IncomeSourceForm.option_property, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.property.routes.PropertyIncomeController.showPropertyIncome().url

          await(goodRequest)
          verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
        }

        s"return a SEE OTHER (303) for both and goto ${controllers.property.routes.PropertyIncomeController.showPropertyIncome().url}" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(IncomeSourceForm.option_both, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.property.routes.PropertyIncomeController.showPropertyIncome().url

          await(goodRequest)
          verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
        }
    }

    "When it is in edit mode" should {
      s"return an SEE OTHER (303) for business and goto ${controllers.routes.SummaryController.showSummary().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe controllers.routes.SummaryController.showSummary().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for property and goto ${controllers.routes.SummaryController.showSummary()}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe controllers.routes.SummaryController.showSummary().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for both and goto ${controllers.routes.SummaryController.showSummary().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe controllers.routes.SummaryController.showSummary().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestIncomeSourceController.submitIncomeSource(isEditMode = true)(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 0)
    }
  }

  authorisationTests

}
