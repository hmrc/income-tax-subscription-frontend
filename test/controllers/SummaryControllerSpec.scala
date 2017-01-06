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

class SummaryControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "SummaryController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestSummaryController.showSummary,
    "submitSummary" -> TestSummaryController.submitSummary
  )

  object TestSummaryController extends SummaryController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
  }

  "The Summary controller" should {
    "use the correct applicationConfig" in {
      SummaryController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      SummaryController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      SummaryController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.showSummary(authenticatedFakeRequest())
    val t = TestSummaryController.showSummary

    "return unimplemented (501)" in {
      status(result) must be(Status.NOT_IMPLEMENTED)
    }
  }

  "Calling the submitSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.submitSummary(authenticatedFakeRequest())

    "return unimplemented (501)" in {
      status(result) must be(Status.NOT_IMPLEMENTED)
    }
  }

  authorisationTests

}
