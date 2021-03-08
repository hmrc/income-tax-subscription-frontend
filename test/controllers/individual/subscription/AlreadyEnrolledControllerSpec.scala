/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.subscription

import agent.audit.mocks.MockAuditingService
import assets.MessageLookup.{AlreadyEnrolled => messages}
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class AlreadyEnrolledControllerSpec extends ControllerBaseSpec with MockAuditingService {

  override val controllerName: String = "AlreadyEnrolledController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "enrolled" -> TestAlreadyEnrolledController.show()
  )

  object TestAlreadyEnrolledController extends AlreadyEnrolledController(
    mockAuditingService,
    mockAuthService
  )

  "Calling the enrolled action of the AlreadyEnrolledController with an enrolled Authenticated User" should {
    "return an OK with the error page" in {
      mockAuthEnrolled()

      lazy val result = TestAlreadyEnrolledController.show(subscriptionRequest)
      lazy val document = Jsoup.parse(contentAsString(result))

      status(result) must be(Status.OK)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title mustBe messages.heading + serviceNameGovUk

      document.select("#sign-out-button").attr("href") mustBe
        controllers.SignOutController.signOut(subscriptionRequest.path).url
    }
  }

  authorisationTests()
}
