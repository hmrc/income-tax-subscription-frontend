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
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class EligibleControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "EligibleController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showEligible" -> TestEligibleController.showEligible,
    "submitEligible" -> TestEligibleController.submitEligible
  )

  object TestEligibleController extends EligibleController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
  }

  "The EligibleController controller" should {
    "use the correct applicationConfig" in {
      TermsController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      TermsController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      TermsController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showTerms action of the TermsController with an authorised user" should {

    lazy val result = TestEligibleController.showEligible(authenticatedFakeRequest())

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user" should {
    lazy val goodRequest = TestEligibleController.submitEligible(authenticatedFakeRequest())

    "return a SEE OTHER (303)" in {
      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest).get mustBe controllers.routes.ContactEmailController.showContactEmail().url
    }
  }

  authorisationTests

}
