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

package controllers.agent

import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.addAnotherClient
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}

class DeclinedSignUpNextYearControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

  s"GET ${routes.DeclinedSignUpNextYearController.show.url}" must {
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"GET ${routes.DeclinedSignUpNextYearController.show.url} is called")
      val result = IncomeTaxSubscriptionFrontend.declinedSignUpNextYear()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("agent.declined-sign-up-next-year.heading") + serviceNameGovUk)
      )
    }
  }

  s"POST ${routes.DeclinedSignUpNextYearController.submit.url}" must {
    "return a SEE_OTHER to the add another client route" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"POST ${routes.DeclinedSignUpNextYearController.submit.url} is called")
      val result = IncomeTaxSubscriptionFrontend.submitDeclinedSignUpNextYear()

      Then("The result should be SEE_OTHER redirecting to the task list page")
      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(addAnotherClient)
      )
    }
  }

}
