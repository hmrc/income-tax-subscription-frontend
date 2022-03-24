/*
 * Copyright 2022 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import auth.individual.SignUp
import config.featureswitch.FeatureSwitch.{SPSEnabled, SaveAndRetrieve}
import controllers.ControllerBaseSpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.ITSASessionKeys
import utilities.individual.TestConstants

import scala.concurrent.Future

class SPSCallbackControllerSpec extends ControllerBaseSpec with MockAuditingService  {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SPSEnabled)
  }

  object TestSPSCallbackController extends SPSCallbackController(
    mockAuditingService,
    mockAuthService
  )

  override val controllerName: String = "PreferencesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "callback" -> TestSPSCallbackController.callback
  )

  def request(hasEntityId: Boolean): Request[_] = {
    val entityIdParam: String = if (hasEntityId) {
      "?entityId=testId"
    } else {
      ""
    }
    FakeRequest("GET", controllers.individual.sps.routes.SPSCallbackController.callback.url + entityIdParam).withSession(
      ITSASessionKeys.JourneyStateKey -> SignUp.name,
      ITSASessionKeys.NINO -> TestConstants.testNino,
      ITSASessionKeys.UTR -> TestConstants.testUtr
    )
  }

  "callback" when {
    "the save and retrieve feature switch is enabled" should {
      "the SPS feature switch is enabled" when {
        "an entityId is passed through to the url" should {
          "save the entityId in session and redirect to the tax year selection page" in {
            enable(SaveAndRetrieve)
            enable(SPSEnabled)

            val result: Future[Result] = TestSPSCallbackController.callback(request(hasEntityId = true)).run()

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.business.routes.TaskListController.show().url)
          }
        }
      }
    }

    "the SPS feature switch is disabled" should {
      "return a not found exception" in {
        val result: Future[Result] = TestSPSCallbackController.callback(request(hasEntityId = true)).run()

        intercept[NotFoundException](await(result)).message mustBe "[SPSCallbackController][callback] - SPS Enabled feature switch not enabled"
      }
    }

    "the save and retrieve feature switch is disabled" should {
      "the SPS feature switch is enabled" when {
        "an entityId is passed through to the url" should {
          "save the entityId in session and redirect to the tax year selection page" in {
            disable(SaveAndRetrieve)
            enable(SPSEnabled)

            val result: Future[Result] = TestSPSCallbackController.callback(request(hasEntityId = true)).run()

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.business.routes.WhatYearToSignUpController.show().url)
          }
        }
        "no entityId is present in the url" should {
          "throw an InternalServerException" in {
            enable(SPSEnabled)

            val result: Future[Result] = TestSPSCallbackController.callback(request(hasEntityId = false)).run()

            intercept[InternalServerException](await(result)).message mustBe "[SPSCallbackController][callback] - Entity Id was not found"
          }
        }
      }
    }
  }

  authorisationTests()

}
