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

import akka.actor._
import akka.stream._
import assets.MessageLookup
import auth._
import config.{FrontendAppConfig, FrontendAuthConnector}
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SummaryControllerSpec extends PlaySpec with OneAppPerTest {

  object TestSummaryController extends SummaryController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
  }

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  "The Summary controller" should {
    "use the correct applicationConfig" in {
      SummaryController.applicationConfig must be (FrontendAppConfig)
    }
    "use the correct authConnector" in {
      SummaryController.authConnector must be (FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      SummaryController.postSignInRedirectUrl must be (FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.showSummary(authenticatedFakeRequest())

    "return unimplemented (501)" in {
      status(result) must be (Status.NOT_IMPLEMENTED)
    }
  }

  "Calling the showSummary action of the SummaryController with an unauthorised user" should {

    lazy val result = TestSummaryController.showSummary(FakeRequest())

    "return 303" in {
      status(result) must be (Status.SEE_OTHER)
    }
  }

  "Calling the submitSummary action of the SummaryController with an authorised user" should {

    lazy val result = TestSummaryController.submitSummary(authenticatedFakeRequest())

    "return unimplemented (501)" in {
      status(result) must be (Status.NOT_IMPLEMENTED)
    }
  }

  "Calling the submitSummary action of the SummaryController with an unauthorised user" should {

    lazy val result = TestSummaryController.submitSummary(FakeRequest())

    "return 303" in {
      status(result) must be (Status.SEE_OTHER)
    }
  }
}
