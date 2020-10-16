/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.usermatching

import assets.MessageLookup.{UserDetailsError => messages}
import auth.individual.UserMatching
import controllers.ControllerBaseSpec
import utilities.individual.TestConstants.{testCredId, testUserId}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, contentType, _}
import uk.gov.hmrc.http.SessionKeys
import utilities.ITSASessionKeys

class UserDetailsErrorControllerSpec extends ControllerBaseSpec {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "UserDetailsErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsErrorController.show,
    "submit" -> TestUserDetailsErrorController.submit
  )

  def createTestUserDetailsErrorController(enableMatchingFeature: Boolean): UserDetailsErrorController = new UserDetailsErrorController(
    mockAuthService)

  lazy val TestUserDetailsErrorController: UserDetailsErrorController = createTestUserDetailsErrorController(enableMatchingFeature = true)

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = userMatchingRequest.withSession(
    SessionKeys.userId -> testCredId, ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the 'show' action of the UserDetailsErrorController" should {

    lazy val result = TestUserDetailsErrorController.show(request)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the 'User Details Error page'" in {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
    }

    s"the page must have a link to sign out" in {
      document.select("#sign-out").attr("href") mustBe
        controllers.SignOutController.signOut(request.path).url
    }

  }

  "Calling the 'submit' action of the UserDetailsErrorController" should {

    lazy val result = TestUserDetailsErrorController.submit(request)

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    "Redirect to the 'User details' page" in {
      redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsController.show().url
    }

  }


  authorisationTests()

}
