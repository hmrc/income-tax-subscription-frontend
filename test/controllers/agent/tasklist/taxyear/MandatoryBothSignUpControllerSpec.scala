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

package controllers.agent.tasklist.taxyear

import config.{AppConfig, MockConfig}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.SessionData
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks._
import views.agent.mocks.MockMandatoryBothSignUp

import scala.concurrent.Future

class MandatoryBothSignUpControllerSpec extends ControllerSpec
  with MockMandatoryBothSignUp
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockAccountingPeriodService {

  implicit val appConfig: AppConfig = MockConfig

  private def testMandatoryBothSignUpController(sessionData: SessionData = SessionData()) = new MandatoryBothSignUpController(
    mandatoryBothSignUp,
    fakeIdentifierActionWithSessionData(sessionData),
    fakeConfirmedClientJourneyRefiner,
    mockAccountingPeriodService
  )(appConfig)

  "show" should {
    "return OK" in {
      mockView()

      val result = testMandatoryBothSignUpController().show(isEditMode = false)(request)

      status(result) mustBe Status.OK
    }
  }

  "submit" when {
    "in edit mode" should {
      "redirect to GlobalCheckYourAnswers" in {
        val result: Future[Result] = testMandatoryBothSignUpController().submit(isEditMode = true)(
          request.withMethod("POST")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
      }
    }
    "not in edit mode" should {
      "redirect to WhatYouNeedToDo" in {
        val result: Future[Result] = testMandatoryBothSignUpController().submit(isEditMode = false)(
          request.withMethod("POST")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
      }
    }
  }
}
