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

import config.AppConfig
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuthStub, PreferencesStub}
import play.api.http.Status.SEE_OTHER

class PreferencesControllerISpec extends ComponentSpecBase {

  override implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "GET /preferences" should {
    "return the preferences page when the user is not activated for preference service" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesStub.stubPaperlessActivated()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a OK with the confirmation page")
      res should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

}
