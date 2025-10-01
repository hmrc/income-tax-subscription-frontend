/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual.tasklist.addbusiness

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

class BusinessAlreadyRemovedControllerISpec extends ComponentSpecBase with AuthRedirects {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.BusinessAlreadyRemovedController.show.url}" must {
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"GET ${routes.BusinessAlreadyRemovedController.show.url} is called")
      val result = IncomeTaxSubscriptionFrontend.showBusinessAlreadyRemoved()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("business-already-removed.heading") + serviceNameGovUk)
      )
    }
  }

  s"GET ${routes.BusinessAlreadyRemovedController.show.url}" when {
    "the user is unauthenticated" should {
      "redirect to login" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.showBusinessAlreadyRemoved()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(ggLoginUrl)
        )
      }
    }
  }

  override val env: Environment = app.injector.instanceOf[Environment]
  override val config: Configuration = app.injector.instanceOf[Configuration]
}
