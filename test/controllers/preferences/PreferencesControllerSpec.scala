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

package controllers.preferences

import auth._
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockPreferencesService

class PreferencesControllerSpec extends ControllerBaseSpec
  with MockPreferencesService {

  override val controllerName: String = "PreferencesControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "checkPreference" -> TestPreferencesController.checkPreference
  )

  object TestPreferencesController extends PreferencesController(
    MockBaseControllerConfig,
    messagesApi,
    TestPreferencesService
  )

  "Calling the checkPreference action of the PreferencesController with an authorised user" should {

    implicit lazy val request = authenticatedFakeRequest()

    def result = TestPreferencesController.checkPreference(request)

    "Return status (200) if paperless is activated" in {
      setupCheckPaperless(paperlessActivated)

      status(result) must be(Status.OK)
    }

    "Redirect to preferences service if paperless is deactivated" in {
      setupCheckPaperless(paperlessDeclined)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(TestPreferencesService.choosePaperlessUrl)
    }

    "Redirect to preferences service if paperless was previously unspecified" in {
      setupCheckPaperless(paperlessPreconditionFailed)

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get must be(TestPreferencesService.choosePaperlessUrl)
    }

  }

  authorisationTests

}
