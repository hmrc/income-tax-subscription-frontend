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

package controllers.individual.tasklist

import auth.individual.{IncomeTaxSAUser, SignUpController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import config.AppConfig
import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import models.common.TaskListModel
import models.common.business.AccountingMethodModel
import models.common.subscription.CreateIncomeSourcesModel
import models.common.subscription.CreateIncomeSourcesModel.createIncomeSources
import play.api.Logging
import play.api.mvc._
import services._
import services.individual.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.tasklist.TaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
                                   mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval with Logging {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withReference { reference =>
        getTaskListModel(reference) map {
          viewModel =>
            Ok(taskListView(
              postAction = controllers.individual.tasklist.routes.TaskListController.submit(),
              viewModel = viewModel,
              accountingPeriodService = accountingPeriodService,
              individualUserNino = user.nino.get,
              IncomeTaxSAUser.fullName,
              utrNumber = user.utr.get
            ))
        }
      }
    }
  }

  private def getTaskListModel(reference: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[TaskListModel] = {
    for {
      businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
      businessAccountingMethod <- subscriptionDetailsService.fetchSelfEmploymentsAccountingMethod(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
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
      incomeSourcesModel =>
        if (isEnabled(EnableTaskListRedesign)) {
          Future.successful(Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show))
        } else {
          val nino = user.nino.get
          val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
          val session = request.session

          subscriptionService.signUpAndCreateIncomeSourcesFromTaskList(
            nino, incomeSourcesModel, maybeSpsEntityId = session.get(SPSEntityId)
          )(headerCarrier) map {
            case Right(_) =>
              Redirect(controllers.individual.routes.ConfirmationController.show)
            case Left(failure) =>
              throw new InternalServerException(s"[TaskListController][submit] - failure response received from submission: ${failure.toString}")
          }
        }
  }

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        withReference { reference =>
          val model = for {
            selfEmployments <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
            selfEmploymentsAccountingMethod <- subscriptionDetailsService.fetchSelfEmploymentsAccountingMethod(reference)
            property <- subscriptionDetailsService.fetchProperty(reference)
            overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
            selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
          } yield {
            createIncomeSources(user.nino.get, selfEmployments, selfEmploymentsAccountingMethod.map(AccountingMethodModel.apply), property, overseasProperty, selectedTaxYear)
          }
          model.flatMap { model =>
            processFunc(user)(request)(model)
          }
        }
    }
}