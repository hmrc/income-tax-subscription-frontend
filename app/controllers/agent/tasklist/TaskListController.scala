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

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.common.TaskListModel
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.tasklist.TaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskListController @Inject()(view: TaskList,
                                   identify: IdentifierAction,
                                   journeyRefiner: ConfirmedClientJourneyRefiner,
                                   subscriptionDetailsService: SubscriptionDetailsService)
                                  (implicit cc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      taskListViewModel <- getTaskListModel(request.reference)
    } yield {
      Ok(view(
        postAction = routes.TaskListController.submit(),
        viewModel = taskListViewModel,
        clientName = request.clientDetails.name,
        clientNino = request.clientDetails.formattedNino,
        clientUtr = request.utr
      ))
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) { _ =>
    Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
  }

  private def getTaskListModel(reference: String)(implicit hc: HeaderCarrier): Future[TaskListModel] = {
    for {
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      incomeSourcesConfirmed <- subscriptionDetailsService.fetchIncomeSourcesConfirmation(reference)
    } yield {
      TaskListModel(
        selectedTaxYear,
        incomeSourcesConfirmed
      )
    }
  }


}