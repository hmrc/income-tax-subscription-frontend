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

package controllers.agent.tasklist.taxyear

import config.{AppConfig, MockConfig}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import views.html.agent.tasklist.taxyear.NonEligibleVoluntary

import scala.concurrent.Future

class NonEligibleVoluntaryControllerSpec
  extends ControllerSpec
    with MockIdentifierAction
    with MockConfirmedClientJourneyRefiner {

  val appConfig: AppConfig = MockConfig

  trait Setup {
    val NonEligibleVoluntary: NonEligibleVoluntary = mock[NonEligibleVoluntary]
    val controller: NonEligibleVoluntaryController = new NonEligibleVoluntaryController(
      NonEligibleVoluntary,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner
    )(appConfig)
  }

  "show" must {
    "return OK with the page content" in new Setup {
      when(NonEligibleVoluntary(
        ArgumentMatchers.eq(routes.NonEligibleVoluntaryController.submit),
        ArgumentMatchers.eq(clientDetails.name),
        ArgumentMatchers.eq(clientDetails.formattedNino),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(any(), any())).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show(
        request
      )

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" must {
    "return SEE_OTHER to the Your Income Sources page" in new Setup {
      val result: Future[Result] = controller.submit(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
    }
  }
}
