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
import forms.TermForm
import models.TermModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class TermsControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "TermsController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showTerms" -> TestTermsController.showTerms,
    "submitTerms" -> TestTermsController.submitTerms
  )

  object TestTermsController extends TermsController (
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showTerms action of the TermsController with an authorised user" should {

    lazy val result = TestTermsController.showTerms(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchTerms = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchTerms = 1, saveTerms = 0)
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user and valid submission" should {

    def callShow = TestTermsController.submitTerms(authenticatedFakeRequest()
      .post(TermForm.termForm, TermModel(true)))

    "return a redirect status (SEE_OTHER - 303)" in {
      setupMockKeystoreSaveFunctions()

      val goodResult = callShow

      status(goodResult) must be(Status.SEE_OTHER)

      await(goodResult)
      verifyKeystore(fetchTerms = 0, saveTerms = 1)
    }

    s"redirect to '${controllers.routes.SummaryController.showSummary().url}'" in {
      setupMockKeystoreSaveFunctions()

      val goodResult = callShow

      redirectLocation(goodResult) mustBe Some(controllers.routes.SummaryController.showSummary().url)

      await(goodResult)
      verifyKeystore(fetchTerms = 0, saveTerms = 1)
    }
  }

  "Calling the submitTerms action of the TermsController with an authorised user and invalid submission" should {
    lazy val badRequest = TestTermsController.submitTerms(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchTerms = 0, saveTerms = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.routes.ContactEmailController.showContactEmail().url}" in {
      TestTermsController.backUrl mustBe controllers.routes.ContactEmailController.showContactEmail().url
    }
  }

  authorisationTests
}
