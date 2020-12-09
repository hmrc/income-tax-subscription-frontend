/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.subscription

import auth.individual.{IncomeTaxSAUser, SignUpController}
import config.AppConfig
import config.featureswitch.FeatureSwitch.{PropertyNextTaxYear, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import javax.inject.{Inject, Singleton}
import models.IndividualSummary
import models.common.IncomeSourceModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.SubscriptionSuccess
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result, _}
import services.individual.SubscriptionOrchestrationService
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil._
import utilities.{ITSASessionKeys, ImplicitDateFormatterImpl}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authService: AuthService,
                                           subscriptionDetailsService: SubscriptionDetailsService,
                                           subscriptionService: SubscriptionOrchestrationService,
                                           incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           implicitDateFormatter: ImplicitDateFormatterImpl)
                                          (implicit val ec: ExecutionContext,
                                           appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def backUrl(incomeSource: IncomeSourceModel): String = {
    incomeSource match {
      case IncomeSourceModel(_, _, true) =>
        controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url
      case IncomeSourceModel(_, true, _) =>
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
      case IncomeSourceModel(true, _, _) =>
        controllers.individual.business.routes.BusinessAccountingMethodController.show().url
    }
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        getSummaryModel(cache).map {
          summaryModel =>
            Ok(views.html.individual.incometax.subscription.check_your_answers(
              summaryModel,
              controllers.individual.subscription.routes.CheckYourAnswersController.submit(),
              backUrl = backUrl(cache.getIncomeSource.get),
              implicitDateFormatter,
              isEnabled(ReleaseFour),
              isEnabled(PropertyNextTaxYear)
            ))
        }
  }

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
        getSummaryModel(cache).flatMap { summaryModel =>
          subscriptionService.createSubscription(nino, summaryModel, isEnabled(ReleaseFour), isEnabled(PropertyNextTaxYear))(headerCarrier).flatMap {
            case Right(SubscriptionSuccess(id)) =>
              subscriptionDetailsService.saveSubscriptionId(id).map(_ => Redirect(controllers.individual.subscription.routes.ConfirmationController.show()))
            case Left(failure) =>
              error("Successful response not received from submission: \n" + failure.toString)
          }
        }
  }

  private def getSummaryModel(cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[IndividualSummary] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
    } yield {
      if (isEnabled(ReleaseFour)) {
        cacheMap.getSummary(businesses, businessAccountingMethod, isReleaseFourEnabled = true, isPropertyNextTaxYearEnabled = isEnabled(PropertyNextTaxYear))
      } else {
        cacheMap.getSummary()
      }
    }
  }

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        subscriptionDetailsService.fetchAll().flatMap { cache =>
          processFunc(user)(request)(cache)
        }
    }

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
