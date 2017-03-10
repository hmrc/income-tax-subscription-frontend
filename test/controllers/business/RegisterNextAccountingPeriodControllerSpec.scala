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

package controllers.business

import auth._
import controllers.ControllerBaseSpec
import forms.RegisterNextAccountingPeriodForm
import models.RegisterNextAccountingPeriodModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

import scala.concurrent.Future

class RegisterNextAccountingPeriodControllerSpec extends ControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "RegisterNextAccountingPeriodController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRegisterNextAccountingPeriodController.show,
    "submit" -> TestRegisterNextAccountingPeriodController.submit
  )

  object TestRegisterNextAccountingPeriodController extends RegisterNextAccountingPeriodController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the show action of the RegisterNextAccountingPeriod with an authorised user" should {

    def result: Future[Result] = {
      setupMockKeystore(fetchRegisterNextAccountingPeriod = None)
      TestRegisterNextAccountingPeriodController.show(authenticatedFakeRequest())
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "retrieve one value from keystore" in {
      await(result)
      verifyKeystore(fetchRegisterNextAccountingPeriod = 1, saveRegisterNextAccountingPeriod = 0)
    }

    s"The back url should point to '${controllers.business.routes.CurrentFinancialPeriodPriorController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe controllers.business.routes.CurrentFinancialPeriodPriorController.show().url
    }
  }

  "Calling the submit action of the RegisterNextAccountingPeriod with an authorised user and valid submission" when {

    def callShow(answer: String): Future[Result] = TestRegisterNextAccountingPeriodController.submit(authenticatedFakeRequest()
      .post(RegisterNextAccountingPeriodForm.registerNextAccountingPeriodForm, RegisterNextAccountingPeriodModel(answer)))

    "Option 'Yes' is selected" should {

      def goodRequest: Future[Result] = {
        setupMockKeystoreSaveFunctions()
        callShow(RegisterNextAccountingPeriodForm.option_yes)
      }

      "return status SEE_OTHER (303)" in {
        status(goodRequest) mustBe Status.SEE_OTHER
      }

      s"redirect to ${controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url}" in {
        redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url
      }

      "save one value into keystore" in {
        await(goodRequest)
        verifyKeystore(fetchRegisterNextAccountingPeriod = 0, saveRegisterNextAccountingPeriod = 1)
      }
    }

    "Option 'No' is selected" should {

      def goodRequest: Future[Result] = {
        setupMockKeystoreSaveFunctions()
        callShow(RegisterNextAccountingPeriodForm.option_no)
      }

      "return status SEE_OTHER (303)" in {
        status(goodRequest) mustBe Status.SEE_OTHER
      }

      s"redirect to ${controllers.routes.ApplicationController.signOut().url}" in {
        redirectLocation(goodRequest).get mustBe controllers.routes.ApplicationController.signOut().url
      }

      "save one value into keystore" in {
        await(goodRequest)
        verifyKeystore(fetchRegisterNextAccountingPeriod = 0, saveRegisterNextAccountingPeriod = 1)
      }
    }
  }

  "Calling the submit action of the RegisterNextAccountingPeriod with an authorised user and invalid submission" should {

    def badRequest: Future[Result] = TestRegisterNextAccountingPeriodController.submit(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)
    }

    "not update or retrieve anything from keystore" in {
      await(badRequest)
      verifyKeystore(fetchRegisterNextAccountingPeriod = 0, saveRegisterNextAccountingPeriod = 0)
    }
  }

  authorisationTests

}
