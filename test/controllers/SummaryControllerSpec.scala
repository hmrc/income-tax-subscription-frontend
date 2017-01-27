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
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class SummaryControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "SummaryController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestSummaryController.showSummary,
    "submitSummary" -> TestSummaryController.submitSummary
  )

  object TestSummaryController extends SummaryController (
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.showSummary(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchAll = TestModels.testCacheMap)

      status(result) must be(Status.OK)
    }
  }

  "Calling the submitSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.submitSummary(authenticatedFakeRequest())

    "return a redirect status (SEE_OTHER - 303)" in {
      status(result) must be(Status.SEE_OTHER)
    }

    s"redirect to '${controllers.routes.ConfirmationController.showConfirmation().url}'" in {
      redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.showConfirmation().url)
    }
  }

  "The back url" should {
    s"point to ${controllers.routes.TermsController.showTerms().url}" in {
      TestSummaryController.backUrl mustBe controllers.routes.TermsController.showTerms().url
    }
  }

  authorisationTests

}
