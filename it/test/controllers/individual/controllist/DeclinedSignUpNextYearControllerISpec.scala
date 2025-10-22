/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.individual.controllist

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.OK

class DeclinedSignUpNextYearControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.DeclinedSignUpNextYearController.show.url}" must {
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"GET ${routes.DeclinedSignUpNextYearController.show.url} is called")
      val result = IncomeTaxSubscriptionFrontend.declinedSignUpNextYear()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("declined-sign-up-next-year.heading") + serviceNameGovUk)
      )
    }
  }

}
