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

package views.agent.tasklist.mocks

import controllers.agent.tasklist.routes
import models.common.TaskListModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.HtmlFormat
import utilities.UserMatchingSessionUtil.ClientDetails
import views.html.agent.tasklist.TaskList

trait MockTaskList extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockView)
  }

  val mockView: TaskList = mock[TaskList]

  def mockTaskList(clientDetails: ClientDetails, utr: String, taskListModel: TaskListModel): Unit = {
    when(mockView(
      ArgumentMatchers.eq(routes.TaskListController.submit()),
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(clientDetails.name),
      ArgumentMatchers.eq(clientDetails.formattedNino),
      ArgumentMatchers.eq(utr)
    )(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(HtmlFormat.empty)
  }

}
