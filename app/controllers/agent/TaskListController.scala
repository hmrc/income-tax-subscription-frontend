/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import common.Constants.ITSASessionKeys
import config.AppConfig
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import models.common.TaskListModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import play.api.Logging
import play.api.mvc._
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import services.agent.SubscriptionOrchestrationService
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil.CacheMapUtil
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.AgentTaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class TaskListController @Inject()(val taskListView: AgentTaskList,
                                   val accountingPeriodService: AccountingPeriodService,
                                   val auditingService: AuditingService,
                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                   val subscriptionService: SubscriptionOrchestrationService,
                                   val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                   val authService: AuthService,
                                   throttlingService: ThrottlingService)
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
        throttlingService.throttled(AgentStartOfJourneyThrottle) {
          getTaskListModel(reference) map {
            viewModel =>
              Ok(taskListView(
                postAction = controllers.agent.routes.TaskListController.submit(),
                viewModel = viewModel,
                clientName = request.fetchClientName.getOrElse(
                  throw new InternalServerException("[TaskListController][show] - could not retrieve client name from session")
                ),
                clientNino = formatNino(user.clientNino.getOrElse(
                  throw new InternalServerException("[TaskListController][show] - could not retrieve client nino from session")
                ))
              ))
          }
        }
      }
    }
  }

  private def getTaskListModel(reference: String)(implicit hc: HeaderCarrier): Future[TaskListModel] = {
    for {
      cacheMap <- subscriptionDetailsService.fetchAll(reference)
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
    } yield {
      cacheMap.getTaskListModel(businesses, businessAccountingMethod, property, overseasProperty)
    }
  }

  def submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      incomeSourceModel =>
        withAgentReference { reference =>
          val nino = user.clientNino.get
          val arn = user.arn.get
          val utr = user.clientUtr.get
          val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

          subscriptionService.createSubscriptionFromTaskList(arn, nino, utr, incomeSourceModel)(headerCarrier).flatMap {
            case Right(SubscriptionSuccess(id)) =>
              subscriptionDetailsService.saveSubscriptionId(reference, id).map { _ =>
                Redirect(controllers.agent.routes.ConfirmationAgentController.show).addingToSession(ITSASessionKeys.MTDITID -> id)
              }
            case Left(failure) =>
              error("Successful response not received from submission: \n" + failure.toString)
          }
        }
  }

  private def journeySafeGuard(processFunc: IncomeTaxAgentUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        throttlingService.throttled(AgentEndOfJourneyThrottle) {
          withAgentReference { reference =>
            val model = for {
              cacheMap <- subscriptionDetailsService.fetchAll(reference)
              selfEmployments <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
              selfEmploymentsAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
              property <- subscriptionDetailsService.fetchProperty(reference)
              overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
            } yield {
              cacheMap.createIncomeSources(user.clientNino.get, selfEmployments, selfEmploymentsAccountingMethod, property, overseasProperty)
            }
            model.flatMap { model =>
              processFunc(user)(request)(model)
            }
          }
        }
    }

  def error(message: String): Future[Nothing] = {
    logger.warn(message)
    Future.failed(new InternalServerException(message))
  }

}
