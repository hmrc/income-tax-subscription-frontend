/*
 * Copyright 2023 HM Revenue & Customs
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

import config.MockConfig
import controllers.individual.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockAuditingService
import views.html.individual.eligibility.DeclinedSignUpNextYear

import scala.concurrent.Future

class DeclinedSignUpNextYearControllerSpec extends ControllerBaseSpec with MockAuditingService {

  override val controllerName: String = "DeclinedSignUpNextYearController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestDeclinedSignUpNextYearController.show
  )

  object TestDeclinedSignUpNextYearController extends DeclinedSignUpNextYearController(
    mock[DeclinedSignUpNextYear]
  )(mockAuditingService, MockConfig, mockAuthService)

  trait Setup {
    val view: DeclinedSignUpNextYear = mock[DeclinedSignUpNextYear]

    val controller: DeclinedSignUpNextYearController = new DeclinedSignUpNextYearController(view)(
      mockAuditingService, MockConfig, mockAuthService
    )
  }

  "show" must {
    "return OK (200) with the page html" in new Setup {
      when(
        view(
          ArgumentMatchers.eq[String](controllers.individual.controllist.routes.CannotSignUpThisYearController.show.url)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show()(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  authorisationTests()

}
