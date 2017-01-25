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
import config.{FrontendAppConfig, FrontendAuthConnector}
import forms.EmailForm
import models.EmailModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class ContactEmailControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "ContactEmailControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showContactEmail" -> TestContactEmailController.showContactEmail,
    "submitContactEmail" -> TestContactEmailController.submitContactEmail
  )

  object TestContactEmailController extends ContactEmailController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The ContactEmailController controller" should {
    "use the correct applicationConfig" in {
      ContactEmailController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      ContactEmailController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      ContactEmailController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showContactEmail action of the ContactEmailController with an authorised user" should {

    lazy val result = TestContactEmailController.showContactEmail(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))


    "return status (200)" in {
      // fetchIncomeSource is required for the back url
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      setupMockKeystore(fetchContactEmail = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchContactEmail = 1, saveContactEmail = 0)
    }

    "render the Contact Email address view" in {
      document.title() mustBe Messages("contact_email.title")
    }
  }

  "Calling the submitContactEmail action of the ContactEmailController with an authorised user and valid submission" should {

    def callShow = TestContactEmailController.submitContactEmail(authenticatedFakeRequest()
      .post(EmailForm.emailForm, EmailModel("test@example.com")))

    "return a redirect status (SEE_OTHER - 303)" in {
      // fetchIncomeSource is required for the back url
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      val goodRequest = callShow
      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchContactEmail = 0, saveContactEmail = 1)
    }

    s"redirect to '${controllers.routes.TermsController.showTerms().url}'" in {
      // fetchIncomeSource is required for the back url
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      val goodRequest = callShow
      redirectLocation(goodRequest) mustBe Some(controllers.routes.TermsController.showTerms().url)

      await(goodRequest)
      verifyKeystore(fetchContactEmail = 0, saveContactEmail = 1)
    }
  }

  "Calling the submitContactEmail action of the ContactEmailController with an authorised user and invalid submission" should {
    lazy val badRequest = TestContactEmailController.submitContactEmail(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      // fetchIncomeSource is required for the back url
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchContactEmail = 0, saveContactEmail = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url} on the business journey" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)
      await(TestContactEmailController.backUrl(FakeRequest())) mustBe controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url
      verifyKeystore(fetchIncomeSource = 1, fetchPropertyIncome = 0)
    }

    s"point to ${controllers.routes.EligibleController.showEligible().url} on the property journey when property income GE10k" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty, fetchPropertyIncome = TestModels.testPropertyIncomeGE10k)
      await(TestContactEmailController.backUrl(FakeRequest())) mustBe controllers.routes.EligibleController.showEligible().url
      verifyKeystore(fetchIncomeSource = 1, fetchPropertyIncome = 1)
    }

    s"point to ${controllers.routes.NotEligibleController.showNotEligible().url} on the property journey when property income LT10k" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty, fetchPropertyIncome = TestModels.testPropertyIncomeLT10k)
      await(TestContactEmailController.backUrl(FakeRequest())) mustBe controllers.routes.NotEligibleController.showNotEligible().url
      verifyKeystore(fetchIncomeSource = 1, fetchPropertyIncome = 1)
    }

    s"point to ${controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url} on the business and property journey" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)
      await(TestContactEmailController.backUrl(FakeRequest())) mustBe controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url
      verifyKeystore(fetchIncomeSource = 1, fetchPropertyIncome = 0)
    }
  }

  authorisationTests

}
