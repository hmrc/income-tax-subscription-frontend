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

package incometax.business.controllers

import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import forms.AccountingPeriodPriorForm
import models.AccountingPeriodPriorModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class BusinessAccountingPeriodPriorControllerSpec extends ControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodPriorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAccountingPeriodPriorController.show(isEditMode = false),
    "submit" -> TestAccountingPeriodPriorController.submit(isEditMode = false)
  )

  object TestAccountingPeriodPriorController extends BusinessAccountingPeriodPriorController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the show action of the BusinessAccountingPeriodPriorController with an authorised user" should {

    def result: Future[Result] = {
      setupMockKeystore(
        fetchAccountingPeriodPrior = None
      )
      TestAccountingPeriodPriorController.show(isEditMode = true)(subscriptionRequest)
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "retrieve one value from keystore" in {
      await(result)
      verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 0)
    }

    s"The back url should point to '${incometax.business.controllers.routes.BusinessNameController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe incometax.business.controllers.routes.BusinessNameController.show().url
    }
  }


  "The back url" should {
    s"point to ${incometax.business.controllers.routes.BusinessNameController.show().url}" in {
      TestAccountingPeriodPriorController.backUrl mustBe incometax.business.controllers.routes.BusinessNameController.show().url
    }
  }

  "Calling the submit action of the BusinessAccountingPeriodPriorController with an authorised user and valid submission" when {

    def callShowCore(answer: String, isEditMode: Boolean): Future[Result] = TestAccountingPeriodPriorController.submit(isEditMode)(subscriptionRequest
      .post(AccountingPeriodPriorForm.accountingPeriodPriorForm, AccountingPeriodPriorModel(answer)))

    "Not in edit mode and " when {
      def callShow(answer: String): Future[Result] = callShowCore(answer, isEditMode = false)

      "Option 'Yes' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }
    }

    "Is in edit mode and " when {
      def callShow(answer: String): Future[Result] = callShowCore(answer, isEditMode = true)

      "Option 'Yes' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'Yes' is selected and there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_yes)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe  incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there were no previous entries" in {
        // this condition shouldn't happen, but no reason to break the journey, just proceed through the journey normally
        setupMockKeystore(fetchAccountingPeriodPrior = None)
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the same as the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }

      "Option 'No' is selected and there there is previous entry and it is the different from the current answer" in {
        setupMockKeystore(fetchAccountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_yes))
        val goodRequest = callShow(AccountingPeriodPriorForm.option_no)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodPrior = 1, saveAccountingPeriodPrior = 1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingPeriodPriorController with an authorised user and invalid submission" should {

    def badRequest: Future[Result] = {
      TestAccountingPeriodPriorController.submit(isEditMode = false)(subscriptionRequest)
    }

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)
    }

    "not update or retrieve anything from keystore" in {
      await(badRequest)
      verifyKeystore(fetchAccountingPeriodPrior = 0, saveAccountingPeriodPrior = 0)
    }
  }

  authorisationTests()

}
