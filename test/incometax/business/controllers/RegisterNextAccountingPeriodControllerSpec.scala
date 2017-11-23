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

package incometax.business.controllers

import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class RegisterNextAccountingPeriodControllerSpec extends ControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "RegisterNextAccountingPeriodController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRegisterNextAccountingPeriodController.show,
    "submit" -> TestRegisterNextAccountingPeriodController.submit
  )

  object TestRegisterNextAccountingPeriodController extends RegisterNextAccountingPeriodController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the show action of the RegisterNextAccountingPeriod with an authorised user" should {

    def result: Future[Result] = {
      TestRegisterNextAccountingPeriodController.show(subscriptionRequest)
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    s"The back url should point to '${incometax.business.controllers.routes.BusinessAccountingPeriodPriorController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe incometax.business.controllers.routes.BusinessAccountingPeriodPriorController.show().url
    }

    s"the page must have a link to sign out" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#sign-out").attr("href") mustBe
        core.controllers.SignOutController.signOut(incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show())(subscriptionRequest).url
    }
  }

  "Calling the submit action of the RegisterNextAccountingPeriod with an authorised user and valid submission" when {

    def callShow(): Future[Result] = TestRegisterNextAccountingPeriodController.submit(subscriptionRequest)

    "Option 'Yes' is selected" should {

      def goodRequest: Future[Result] = {
        setupMockKeystoreSaveFunctions()
        callShow()
      }

      "return status SEE_OTHER (303)" in {
        status(goodRequest) mustBe Status.SEE_OTHER
      }

      s"redirect to ${incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url}" in {
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
      }

    }
  }

  authorisationTests()

}
