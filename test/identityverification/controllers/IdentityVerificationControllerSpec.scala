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

package identityverification.controllers

import assets.MessageLookup
import core.audit.Logging
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class IdentityVerificationControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "IdentityVerificationController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "gotoIV" -> TestIdentityVerificationController.gotoIV
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
    implicit lazy val request = subscriptionRequest

    "completionUri sets the correctly uri without a query string" in {
      val url = IdentityVerificationController.completionUri(baseUrl)
      url must endWith(controllers.routes.HomeController.index().url)
    }
    "failureUri sets the correctly uri without a query string" in {
      val url = IdentityVerificationController.failureUri(baseUrl)
      url must endWith(identityverification.controllers.routes.IdentityVerificationController.ivFailed().url)
    }
  }

  "Calling the gotoIV action with a user without a nino" should {
    "return an SEE OTHER to the identity verification frontend" in {
      mockIndividualWithNoEnrolments()

      val request = subscriptionRequest

      lazy val result = TestIdentityVerificationController.gotoIV(request)

      status(result) must be(Status.SEE_OTHER)
      val redirection = redirectLocation(result).get
      redirection must include regex """^/mdtp/uplift\?origin=MTDIT&confidenceLevel=([0-9]+?)&completionURL=(.+?)&failureURL=(.+?)$"""
      redirection must include("/mdtp/uplift?")
      redirection must include("&confidenceLevel=200")
      val cUrl = IdentityVerificationController.completionUri(baseUrl)
      redirection must include regex s"&completionURL=(.*?)$cUrl"
      val fUrl = IdentityVerificationController.failureUri(baseUrl)
      redirection must include regex s"&failureURL=(.*?)$fUrl"
    }
  }

  "calling the ivError action" should {
    "return the iv_failed page" in {
      val request = subscriptionRequest

      lazy val result = await(TestIdentityVerificationController.ivFailed(request))
      lazy val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe Status.OK
      document.title() mustBe MessageLookup.IvFailed.title
    }
  }

  authorisationTests()

}
