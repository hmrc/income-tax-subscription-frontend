/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.business

import controllers.ControllerBaseSpec
import forms.individual.business.BusinessNameForm
import models.individual.business.BusinessNameModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockKeystoreService

import scala.concurrent.Future

class BusinessNameControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessNameController.show(isEditMode = false),
    "submit" -> TestBusinessNameController.submit(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    mockAuthService,
    messagesApi,
    MockKeystoreService
  )

  "Calling the show action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(
        fetchBusinessName = None
      )

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessName = 1, saveBusinessName = 0)

    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user on the sign up journey and valid submission" should {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) '${controllers.individual.business.routes.MatchTaxYearController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.MatchTaxYearController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }

    "When it is in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user on the registration journey and valid submission" should {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        registrationRequest
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) redirect to '${controllers.individual.business.routes.BusinessPhoneNumberController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessPhoneNumberController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }

    "When it is in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchBusinessName = 0, saveBusinessName = 0)
    }
  }

  "The back url" when {
    "in edit mode" should {
      s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessNameController.backUrl(isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    }
    "not in edit mode" should {
      s"redirect to ${controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url}" in {
        TestBusinessNameController.backUrl(isEditMode = false) mustBe controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
      }
    }
  }

  authorisationTests()

}
