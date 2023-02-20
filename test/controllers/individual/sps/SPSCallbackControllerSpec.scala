/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.sps

import auth.individual.SignUp
import common.Constants.ITSASessionKeys
import controllers.ControllerBaseSpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockAuditingService
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.TestConstants

import scala.concurrent.Future

class SPSCallbackControllerSpec extends ControllerBaseSpec with MockAuditingService {

  object TestSPSCallbackController extends SPSCallbackController(
    mockAuditingService,
    mockAuthService
  )

  override val controllerName: String = "PreferencesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "callback" -> TestSPSCallbackController.callback(None)
  )

  def request(hasEntityId: Boolean): Request[_] = {
    val entityIdParam: String = if (hasEntityId) {
      "?entityId=testId"
    } else {
      ""
    }
    FakeRequest("GET", controllers.individual.sps.routes.SPSCallbackController.callback(None).url + entityIdParam).withSession(
      ITSASessionKeys.JourneyStateKey -> SignUp.name,
      ITSASessionKeys.NINO -> TestConstants.testNino,
      ITSASessionKeys.UTR -> TestConstants.testUtr
    )
  }

  "callback" should {
    "save the entityId in session and redirect to the task list page" when {
      "an entityId is passed through to the url" in {
        val result: Future[Result] = TestSPSCallbackController.callback(Some("testEntityId"))(request(hasEntityId = true)).run()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
      }
    }

    "throw an InternalServerException" when {
      "no entityId is present in the url" in {
        val result: Future[Result] = TestSPSCallbackController.callback(None)(request(hasEntityId = false)).run()

        intercept[InternalServerException](await(result)).message mustBe "[SPSCallbackController][callback] - Entity Id was not found"
      }
    }
  }

  authorisationTests()
}
