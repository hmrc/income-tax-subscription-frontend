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

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.TaskListModel
import play.api.Logging
import play.api.mvc._
import services._
import services.agent.ClientDetailsRetrieval
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.tasklist.TaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class TaskListController @Inject()(taskListView: TaskList,
                                   clientDetailsRetrieval: ClientDetailsRetrieval,
                                   referenceRetrieval: ReferenceRetrieval,
                                   utrService: UTRService,
                                   subscriptionDetailsService: SubscriptionDetailsService)
                                  (val auditingService: AuditingService,
                                   val authService: AuthService)
                                  (implicit val ec: ExecutionContext,
                                   val appConfig: AppConfig,
                                   mcc: MessagesControllerComponents) extends AuthenticatedController with Logging {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }


  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      for {
        reference <- referenceRetrieval.getAgentReference
        clientDetails <- clientDetailsRetrieval.getClientDetails
        viewModel <- getTaskListModel(reference)
        utr <- utrService.getUTR
      } yield {
        Ok(taskListView(
          postAction = controllers.agent.tasklist.routes.TaskListController.submit(),
          viewModel = viewModel,
          clientName = clientDetails.name,
          clientNino = formatNino(clientDetails.nino),
          clientUtr = utr
        ))
      }
    }
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

  def submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
  }

}

