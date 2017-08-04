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

package controllers.iv

import audit.Logging
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Call}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolments

class IdentityVerificationControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "IdentityVerificationController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "gotoIV" -> TestIdentityVerificationController.gotoIV,
    "callback" -> TestIdentityVerificationController.callback(""),
    "failureCallBack" -> TestIdentityVerificationController.failureCallBack("")
  )

  object TestIdentityVerificationController extends IdentityVerificationController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  // not a real journey id and its value doesn't really matter for unit test purposes
  val testJourneyId = "testJourneyId"
  lazy val baseUrl: String = appConfig.baseUrl

  "IdentityVerificationController's url functions" should {
    implicit lazy val request = fakeRequest

    "removeQueryString should remove the query strings from a Call" in {
      val testRoute = "/example.com"
      val call: Call = Call("GET", s"$testRoute?query=ok")

      import IdentityVerificationController.removeQueryString
      val url = removeQueryString(baseUrl, call)
      url must be(testRoute)
    }
    "completionUri sets the correctly uri without a query string" in {
      val url = IdentityVerificationController.completionUri(baseUrl)
      url must endWith("/iv/callback")
    }
    "failureUri sets the correctly uri without a query string" in {
      val url = IdentityVerificationController.failureUri(baseUrl)
      url must endWith("/iv/failure")
    }
  }

  "Calling the gotoIV action of the IdentityVerificationController with a user without a nino" should {
    "return an SEE OTHER to the identity verification frontend" in {
      mockRetrievalSuccess(Enrolments(Set.empty))

      // TODO use a more fitting auth request when it's defined
      val request = fakeRequest

      lazy val result = TestIdentityVerificationController.gotoIV(request)

      status(result) must be(Status.SEE_OTHER)
      val redirection = redirectLocation(result).get
      redirection must include regex """^/mdtp/uplift\?origin=mtd-itsa&confidenceLevel=([0-9]+?)&completionURL=(.+?)&failureURL=(.+?)$"""
      redirection must include("/mdtp/uplift?")
      redirection must include("&confidenceLevel=200")
      val cUrl = IdentityVerificationController.completionUri(baseUrl)(request)
      redirection must include regex s"&completionURL=(.*?)$cUrl"
      val fUrl = IdentityVerificationController.failureUri(baseUrl)(request)
      redirection must include regex s"&failureURL=(.*?)$fUrl"
    }
  }

  "Calling the callback action of the IdentityVerificationController with an Authenticated User" should {
    "return an SEE OTHER to the index page" in {
      lazy val result = TestIdentityVerificationController.callback(testJourneyId)(fakeRequest)

      status(result) must be(Status.SEE_OTHER)
      val redirection = redirectLocation(result).get

      redirection mustBe controllers.routes.HomeController.index().url
    }
  }

  "Calling the failureCallBack action of the IdentityVerificationController with a User" should {
    "return an SEE OTHER to the no nino page" in {
      // TODO use a more fitting auth request when it's defined
      val request = fakeRequest

      lazy val result = TestIdentityVerificationController.failureCallBack(testJourneyId)(request)

      status(result) must be(Status.SEE_OTHER)
      val redirection = redirectLocation(result).get

      redirection mustBe controllers.routes.NoNinoController.showNoNino().url
    }
  }

  authorisationTests()

}
