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

package controllers.agent.tasklist

import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.Current
import models.common.{AccountingYearModel, TaskListModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import views.agent.tasklist.mocks.MockTaskList

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerSpec
  with MockTaskList
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService {

  "show" should {
    "return OK with the task list page" in {
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
      mockFetchIncomeSourceConfirmation(Some(true))
      mockTaskList(
        clientDetails = clientDetails,
        utr = utr,
        taskListModel = TaskListModel(
          taxYearSelection = Some(AccountingYearModel(Current)),
          incomeSourcesConfirmed = Some(true)
        )
      )

      val result: Future[Result] = TestTaskListController.show()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" when {
    "redirect to the global check your answers page" in {
      val result: Future[Result] = TestTaskListController.submit()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
    }
  }

  object TestTaskListController extends TaskListController(
    mockView,
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService
  )

}