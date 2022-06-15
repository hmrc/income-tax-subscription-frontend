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

package controllers.individual.business

import auth.individual.{IncomeTaxSAUser, SignUpController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import models.common.TaskListModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import play.api.Logging
import play.api.mvc._
import services._
import services.individual.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil.CacheMapUtil
import views.html.individual.incometax.business.TaskList

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskListController @Inject()(val taskListView: TaskList,
                                   val accountingPeriodService: AccountingPeriodService,
                                   val auditingService: AuditingService,
                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                   val subscriptionService: SubscriptionOrchestrationService,
                                   val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                   val authService: AuthService,
                                   throttlingService: ThrottlingService)
                                  (implicit val ec: ExecutionContext,
                                   val appConfig: AppConfig,
                                   mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval with Logging {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      if (isEnabled(SaveAndRetrieve)) {
        withReference { reference =>
          getTaskListModel(reference) map {
            viewModel =>
              Ok(taskListView(
                postAction = controllers.individual.business.routes.TaskListController.submit(),
                viewModel = viewModel,
                accountingPeriodService = accountingPeriodService,
                individualUserNino = user.nino.get
              ))
          }
        }
      } else {
        Future.failed(new NotFoundException("[TaskListController][show] - The save and retrieve feature switch is disabled"))
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
      incomeSourcesModel =>
        withReference { reference =>
          val nino = user.nino.get
          val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
          val session = request.session

          subscriptionService.signUpAndCreateIncomeSourcesFromTaskList(
            nino, incomeSourcesModel, maybeSpsEntityId = session.get(SPSEntityId)
          )(headerCarrier).flatMap {
            case Right(SubscriptionSuccess(id)) =>
              subscriptionDetailsService.saveSubscriptionId(reference, id).map { _ =>
                Redirect(controllers.individual.subscription.routes.ConfirmationController.show)
              }
            case Left(failure) =>
              error("Successful response not received from submission: \n" + failure.toString)
          }
        }

  }

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        throttlingService.throttled(IndividualEndOfJourneyThrottle) {
          if (isEnabled(SaveAndRetrieve)) {
            withReference { reference =>
              val model = for {
                cacheMap <- subscriptionDetailsService.fetchAll(reference)
                selfEmployments <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
                selfEmploymentsAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
                property <- subscriptionDetailsService.fetchProperty(reference)
                overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
              } yield {
                cacheMap.createIncomeSources(user.nino.get, selfEmployments, selfEmploymentsAccountingMethod, property, overseasProperty)
              }
              model.flatMap { model =>
                processFunc(user)(request)(model)
              }
            }
          } else {
            throw new NotFoundException("[TaskListController][submit] - The save and retrieve feature switch is disabled")
          }
        }
    }

  def error(message: String): Future[Nothing] = {
    logger.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
