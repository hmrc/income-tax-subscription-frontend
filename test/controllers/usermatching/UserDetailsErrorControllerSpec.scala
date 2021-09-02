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

package controllers.usermatching

import agent.audit.mocks.MockAuditingService
import assets.MessageLookup.{UserDetailsError => messages}
import auth.individual.UserMatching
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, _}
import play.twirl.api.HtmlFormat
import utilities.ITSASessionKeys
import views.html.individual.usermatching.UserDetailsError

import scala.concurrent.Future

class UserDetailsErrorControllerSpec extends ControllerBaseSpec with MockAuditingService {

  val mockUserDetailsError: UserDetailsError = mock[UserDetailsError]
  when(mockUserDetailsError(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestUserDetailsErrorController extends UserDetailsErrorController(
    mockAuditingService,
    mockAuthService,
    mockUserDetailsError
  )

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "UserDetailsErrorController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsErrorController.show,
    "submit" -> TestUserDetailsErrorController.submit
  )

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = userMatchingRequest.withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the 'show' action of the UserDetailsErrorController" should {
    def call(request: Request[AnyContent]): Future[Result] = TestUserDetailsErrorController.show(request)

    lazy val result = call(request)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
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
