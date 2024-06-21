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

package controllers.individual

import common.Constants.ITSASessionKeys
import models.status.MandationStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockMandationStatusService}
import utilities.individual.TestConstants.{testNino, testUtr}
import views.html.individual.WhatYouNeedToDo

import scala.concurrent.Future

class WhatYouNeedToDoControllerSpec extends ControllerBaseSpec with MockAuditingService with MockMandationStatusService {

  object TestWhatYouNeedToDoController extends WhatYouNeedToDoController(
    mock[WhatYouNeedToDo],
    mockMandationStatusService
  )(
    mockAuditingService,
    appConfig,
    mockAuthService
  )

  trait Setup {
    val whatYouNeedToDo: WhatYouNeedToDo = mock[WhatYouNeedToDo]
    val controller: WhatYouNeedToDoController = new WhatYouNeedToDoController(whatYouNeedToDo, mockMandationStatusService)(
      mockAuditingService,
      appConfig,
      mockAuthService
    )
  }

  override val controllerName: String = "WhatYouNeedToDoController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYouNeedToDoController.show,
    "submit" -> TestWhatYouNeedToDoController.submit
  )

  "show" must {
    "return OK with the page content" when {
      "the session contains mandated and eligible for next year only flag of true" in new Setup {
        mockGetMandationService(testNino, testUtr)(Voluntary, Mandated)

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true)
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          subscriptionRequest.withSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true")
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible only next year flag of false" in new Setup {
        mockGetMandationService(testNino, testUtr)(Voluntary, Voluntary)

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false)
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest.withSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "false"))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible only next year flag of true" in new Setup {
        mockGetMandationService(testNino, testUtr)(Voluntary, Voluntary)

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false)
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest.withSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "true"))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a mandated current year flag of true" in new Setup {
        mockGetMandationService(testNino, testUtr)(Mandated, Voluntary)

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false)
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }

    "submit" must {
      "return SEE_OTHER to the task list page" in new Setup {
        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)
      }
    }

  }
}