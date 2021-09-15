/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.Assets.{OK, SEE_OTHER}
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import views.html.individual.incometax.business.TaskList

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerBaseSpec with MockAuditingService with FeatureSwitching{

  override val controllerName: String = "TaskListController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTaskListController.show,
    "submit" -> TestTaskListController.submit
  )

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  def taskListView = {
    val mockTaskListView: TaskList = mock[TaskList]
    when(mockTaskListView(any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)
    mockTaskListView
  }

  object TestTaskListController extends TaskListController(
    taskListView,
    mockAuditingService,
    mockAuthService,
  )


  "show" should {
    "return an OK status with the task list page" in {
      enable(SaveAndRetrieve)
      val result: Future[Result] = TestTaskListController.show()(subscriptionRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "return SEE_OTHER status when unauthorised" in {
      enable(SaveAndRetrieve)
      val result: Future[Result] = TestTaskListController.show()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }

    "Throw an exception if feature not enabled" in {
      disable(SaveAndRetrieve)
      val result: Future[Result] = await(TestTaskListController.show()(subscriptionRequest))
      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]    }
  }

  "submit" should {
    "redirect somewhere" in {
      enable(SaveAndRetrieve)
      val result: Future[Result] = TestTaskListController.submit()(subscriptionRequest)
      status(result) mustBe SEE_OTHER
    }
  }
}
