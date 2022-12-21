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

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import models.common.TaskListModel
import models.common.business.AccountingMethodModel
import models.common.subscription.CreateIncomeSourcesModel.createIncomeSources
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import play.api.Logging
import play.api.mvc._
import services._
import services.agent.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.TaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class TaskListController @Inject()(val taskListView: TaskList,
                                   val accountingPeriodService: AccountingPeriodService,
                                   val auditingService: AuditingService,
                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                   val subscriptionService: SubscriptionOrchestrationService,
                                   val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                   val authService: AuthService)
                                  (implicit val ec: ExecutionContext,
                                   val appConfig: AppConfig,
                                   mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval with Logging {

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
      withAgentReference { reference =>
        getTaskListModel(reference) map {
          viewModel =>
            Ok(taskListView(
              postAction = controllers.agent.tasklist.routes.TaskListController.submit(),
              viewModel = viewModel,
              clientName = request.fetchClientName.getOrElse(
                throw new InternalServerException("[TaskListController][show] - could not retrieve client name from session")
              ),
              clientNino = formatNino(user.clientNino.getOrElse(
                throw new InternalServerException("[TaskListController][show] - could not retrieve client nino from session")
              )),
              clientUtr = user.clientUtr.getOrElse(
                throw new InternalServerException("[TaskListController][show] - could not retrieve client utr from session")
              )
            ))
        }
      }
    }
  }

  private def getTaskListModel(reference: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[TaskListModel] = {
    for {
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
      businessAccountingMethod <- subscriptionDetailsService.fetchSelfEmploymentsAccountingMethod(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      incomeSourcesConfirmed <- subscriptionDetailsService.fetchIncomeSourcesConfirmation(reference)
    } yield {
      TaskListModel(
        selectedTaxYear,
        businesses,
        businessAccountingMethod,
        property,
        overseasProperty,
        incomeSourcesConfirmed
      )
    }
  }

  def submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      incomeSourceModel =>
        if (isEnabled(EnableTaskListRedesign)) {
          Future.successful(Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show))
        } else {
          val nino = user.clientNino.get
          val arn = user.arn
          val utr = user.clientUtr.get
          val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

          subscriptionService.createSubscriptionFromTaskList(arn, nino, utr, incomeSourceModel)(headerCarrier) map {
            case Right(SubscriptionSuccess(id)) =>
              Redirect(controllers.agent.routes.ConfirmationController.show).addingToSession(ITSASessionKeys.MTDITID -> id)
            case Left(failure) =>
              throw new InternalServerException(s"[TaskListController][submit] - failure response received from submission: ${failure.toString}")
          }
        }
  }

  private def journeySafeGuard(processFunc: IncomeTaxAgentUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        withAgentReference { reference =>
          val model = for {
            selfEmployments <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
            selfEmploymentsAccountingMethod <- subscriptionDetailsService.fetchSelfEmploymentsAccountingMethod(reference)
            property <- subscriptionDetailsService.fetchProperty(reference)
            overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
            selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
          } yield {
            createIncomeSources(
              nino = user.clientNino.get,
              selfEmployments = selfEmployments,
              selfEmploymentsAccountingMethod = selfEmploymentsAccountingMethod.map(AccountingMethodModel.apply),
              property = property,
              overseasProperty = overseasProperty,
              accountingYear = selectedTaxYear
            )
          }
          model.flatMap { model =>
            processFunc(user)(request)(model)
          }
        }
    }

}

