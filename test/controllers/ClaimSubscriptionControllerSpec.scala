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

import assets.MessageLookup.{AlreadyEnrolled => messages}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class ClaimSubscriptionControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "ClaimSubscriptionController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "claim" -> TestClaimSubscriptionController.claim()
  )

  object TestClaimSubscriptionController extends ClaimSubscriptionController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService
  )

  "Calling the claim action of the ClaimSubscriptionController with a subscribed Authenticated User" should {
    "return an OK with the error page" in {

      lazy val result = TestClaimSubscriptionController.claim(fakeRequest)
      lazy val document = Jsoup.parse(contentAsString(result))

      status(result) must be(Status.OK)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      document.title mustBe messages.heading

      document.select("form").attr("action") mustBe controllers.routes.SignOutController.signOut().url
    }
  }

  authorisationTests()
}
