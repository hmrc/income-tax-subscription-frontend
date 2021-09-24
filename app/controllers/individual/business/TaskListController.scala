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

import auth.individual.{IncomeTaxSAUser, SignUpController}
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import models.common.{IncomeSourceModel, TaskListModel}
import play.api.Logger
import play.api.mvc._
import services.individual.SubscriptionOrchestrationService
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import utilities.ITSASessionKeys
import utilities.ITSASessionKeys.SPSEntityId
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
                                   val authService: AuthService)
                                  (implicit val ec: ExecutionContext,
                                   val appConfig: AppConfig,
                                   mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {


  private def withSubscriptionDetails(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        subscriptionDetailsService.fetchAll().flatMap { cache =>
          processFunc(user)(request)(cache)
        }
    }

  val show: Action[AnyContent] = withSubscriptionDetails { implicit user =>
    implicit request => {
      cache =>
        getTaskListModel(cache).map {
          viewModel =>
            if (isEnabled(SaveAndRetrieve)) {
              Ok(taskListView(
                postAction = controllers.individual.business.routes.TaskListController.submit(),
                viewModel = viewModel,
                accountingPeriodService = accountingPeriodService
              ))
            } else {
              throw new NotFoundException("[TaskListController][show] - The save and retrieve feature switch is disabled")
            }
        }
    }
  }

  private def getTaskListModel(cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[TaskListModel] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
    } yield {
      cacheMap.getTaskListModel(businesses, businessAccountingMethod)
    }
  }

  def submit: Action[AnyContent] = journeySafeGuard { implicit user =>

    implicit request =>
      incomeSourcesModel =>

        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
        val session = request.session

        subscriptionService.signUpAndCreateIncomeSourcesFromTaskList(nino, incomeSourcesModel, maybeSpsEntityId = session.get(SPSEntityId))(headerCarrier).flatMap {
          case Right(SubscriptionSuccess(id)) =>
            subscriptionDetailsService.saveSubscriptionId(id).map(_ => Redirect(controllers.individual.subscription.routes.ConfirmationController.show()))
          case Left(failure) =>
            error("Successful response not received from submission: \n" + failure.toString)
        }
  }

  private[controllers] def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CreateIncomeSourcesModel => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (isEnabled(SaveAndRetrieve)) {
          val model = for {
            cacheMap <- subscriptionDetailsService.fetchAll()
            selfEmployments <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
            selfEmploymentsAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
          } yield {
            cacheMap.createIncomeSources(user.nino.get, selfEmployments, selfEmploymentsAccountingMethod)
          }
          model.flatMap { model =>
            processFunc(user)(request)(model)
          }
        } else {
          throw new NotFoundException("[TaskListController][submit] - The save and retrieve feature switch is disabled")
        }

    }

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }

}