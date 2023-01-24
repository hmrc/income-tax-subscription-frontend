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

package controllers.agent

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import common.Constants.ITSASessionKeys
import config.AppConfig
import connectors.IncomeTaxSubscriptionConnector
import controllers.agent.TaskListController.interpretTaxYearAsEditableOrNot
import controllers.utils.ReferenceRetrieval
import models.Next
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.CreateIncomeSourcesModel.createIncomeSources
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import models.common.{AccountingYearModel, TaskListModel}
import play.api.Logging
import play.api.mvc._
import services._
import services.agent.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
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

  private def getTaskListModel(reference: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[TaskListModel] = {
    for {
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      eligibilityNextYearOnly = getEligibilityNextYearOnlyFromSession
    } yield {
      TaskListModel(
        interpretTaxYearAsEditableOrNot(selectedTaxYear, eligibilityNextYearOnly),
        businesses,
        businessAccountingMethod.map(_.accountingMethod),
        property,
        overseasProperty
      )
    }
  }

  private def getEligibilityNextYearOnlyFromSession(implicit request: Request[AnyContent]) =
    request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).exists(_.toBoolean)

  def submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      incomeSourceModel =>
        val nino = user.clientNino.get
        val arn = user.arn.get
        val utr = user.clientUtr.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

        subscriptionService.createSubscriptionFromTaskList(arn, nino, utr, incomeSourceModel)(headerCarrier) map {
          case Right(SubscriptionSuccess(id)) =>
            Redirect(controllers.agent.routes.ConfirmationAgentController.show).addingToSession(ITSASessionKeys.MTDITID -> id)
          case Left(failure) =>
            throw new InternalServerException(s"[TaskListController][submit] - failure response received from submission: ${failure.toString}")
        }
  }

  private def journeySafeGuard(processFunc: IncomeTaxAgentUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        withAgentReference { reference =>
          val model = for {
            selfEmployments <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
            selfEmploymentsAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
            property <- subscriptionDetailsService.fetchProperty(reference)
            overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
            selectedTaxYear <- fetchSelectedTaxYear(reference)
          } yield {
            createIncomeSources(user.clientNino.get, selfEmployments, selfEmploymentsAccountingMethod, property, overseasProperty, accountingYear = selectedTaxYear)
          }
          model.flatMap { model =>
            processFunc(user)(request)(model)
          }
        }
    }

  def fetchSelectedTaxYear(reference: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Option[AccountingYearModel]] =
    if (getEligibilityNextYearOnlyFromSession) {
      Future.successful(Some(AccountingYearModel(Next, confirmed = true, editable = false)))
    } else
      subscriptionDetailsService.fetchSelectedTaxYear(reference)

}

object TaskListController {
  /*
  Note 1, no matter what has been retrieved from storage, the editable value will always be newly set.

  Note 2, the business rules state that "allowed this year" will always imply "allowed next year" so we only
  have to deal with the cases where (1) this year is not allowed but next year is, or (2) both are allowed.  If
  neither are allowed the user is not permitted to go to the task list at all.
   */
  private[agent] def interpretTaxYearAsEditableOrNot(
                                                      selectedTaxYear: Option[AccountingYearModel],
                                                      eligibilityNextYearOnly: Boolean): Option[AccountingYearModel] =
    if (eligibilityNextYearOnly) Some(AccountingYearModel(Next, editable = false))
    else selectedTaxYear.map(_.copy(editable = true)) // whatever year was previously specified, and editing must be allowed

}
