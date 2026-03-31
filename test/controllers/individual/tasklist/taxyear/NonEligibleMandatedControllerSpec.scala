/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.individual.tasklist.taxyear

import config.{AppConfig, MockConfig}
import controllers.individual.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.individual.mocks.MockAuthService
import services.mocks.MockAuditingService
import views.html.individual.tasklist.taxyear.NonEligibleMandated

import scala.concurrent.Future

class NonEligibleMandatedControllerSpec
  extends ControllerBaseSpec
    with MockAuditingService
    with MockAuthService {

  override val appConfig: AppConfig = MockConfig

  object TestNonEligibleMandatedController extends NonEligibleMandatedController(
    mock[NonEligibleMandated]
  )(
    mockAuditingService,
    appConfig,
    mockAuthService
  )

  trait Setup {
    val NonEligibleMandated: NonEligibleMandated = mock[NonEligibleMandated]
    val controller: NonEligibleMandatedController = new NonEligibleMandatedController(
      NonEligibleMandated
    )(
      mockAuditingService,
      appConfig,
      mockAuthService
    )
  }

  override val controllerName: String = "WhatYouNeedToDoController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestNonEligibleMandatedController.show,
    "submit" -> TestNonEligibleMandatedController.submit
  )

  "show" must {
    "return OK with the page content" in new Setup {
      when(NonEligibleMandated(
        ArgumentMatchers.eq(routes.NonEligibleMandatedController.submit),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(any(), any())).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show(
        subscriptionRequest
      )

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" must {
    "return SEE_OTHER to the Your Income Sources page" in new Setup {
      val result: Future[Result] = controller.submit(
        subscriptionRequest
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
    }
  }
}
