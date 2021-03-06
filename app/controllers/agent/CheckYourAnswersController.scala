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

package controllers.agent

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.AgentAnswers._
import controllers.utils.RequireAnswer
import javax.inject.{Inject, Singleton}
import models.common.IncomeSourceModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.subscription.SubscriptionSuccess
import play.api.Logger
import play.api.mvc._
import services.agent.SubscriptionOrchestrationService
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.ImplicitDateFormatterImpl
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           subscriptionService: SubscriptionOrchestrationService,
                                           incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           implicitDateFormatter: ImplicitDateFormatterImpl)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends AuthenticatedController with FeatureSwitching with RequireAnswer {


  private def journeySafeGuard(processFunc: => IncomeTaxAgentUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (user.clientNino.isDefined && user.clientUtr.isDefined) {
          subscriptionDetailsService.fetchAll().flatMap {
            case cache => processFunc(user)(request)(cache)
            case _ => error(noCacheMapErrMessage)
          }
        } else {
          Future.successful(Redirect(controllers.agent.matching.routes.ConfirmClientController.show()))
        }
    }

  def backUrl(incomeSource: IncomeSourceModel): String = {
    incomeSource match {
      case IncomeSourceModel(_, _, true) =>
        controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show().url
      case IncomeSourceModel(_, true, _) =>
        controllers.agent.business.routes.PropertyAccountingMethodController.show().url
      case _ =>
        controllers.agent.business.routes.BusinessAccountingMethodController.show().url
    }
  }

  private def getSelfEmploymentsData()(implicit request: Request[AnyContent]): Future[(Option[Seq[SelfEmploymentData]], Option[AccountingMethodModel])] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
    } yield (businesses, businessAccountingMethod)
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        if (isEnabled(ReleaseFour)) {
          require(incomeSourceModelAnswer) { incomeSource =>
            for {
              (businesses, businessAccountingMethod) <- getSelfEmploymentsData()
            } yield {
              Ok(views.html.agent.check_your_answers(
                cache.getAgentSummary(businesses, businessAccountingMethod, true),
                controllers.agent.routes.CheckYourAnswersController.submit(),
                backUrl = backUrl(incomeSource),
                implicitDateFormatter,
                true
              ))
            }
          }
        } else {
          require(incomeSourceModelAnswer) { incomeSource =>
            Future.successful(Ok(views.html.agent.check_your_answers(
              cache.getAgentSummary(),
              controllers.agent.routes.CheckYourAnswersController.submit(),
              backUrl = backUrl(incomeSource),
              implicitDateFormatter,
              false
            )))
          }
        }
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any Subscription Details  cached data")

  private def submitForAuthorisedAgent(arn: String, nino: String, utr: String)
                                      (implicit user: IncomeTaxAgentUser, request: Request[AnyContent], cache: CacheMap): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    for {
      mtditid <- subscriptionService.createSubscription(arn = arn, nino = nino, utr = utr, summaryModel = cache.getAgentSummary())(headerCarrier)
        .collect { case Right(SubscriptionSuccess(id)) => id }
        .recoverWith { case _ => error("Successful response not received from submission") }
      _ <- subscriptionDetailsService.saveSubscriptionId(mtditid)
        .recoverWith { case _ => error("Failed to save to Subscription Details ") }
    } yield Redirect(controllers.agent.routes.ConfirmationController.show()).addingToSession(ITSASessionKeys.MTDITID -> mtditid)
  }

  private def submitForAuthorisedAgentWithReleaseFourEnabled(arn: String, nino: String, utr: String)
                                                            (implicit user: IncomeTaxAgentUser, request: Request[AnyContent],
                                                             cache: CacheMap): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    for {
      (businesses, businessAccountingMethod) <- getSelfEmploymentsData()
      mtditid <- subscriptionService.createSubscription(arn = arn, nino = nino, utr = utr,
        summaryModel = cache.getAgentSummary(businesses, businessAccountingMethod, true), true)(headerCarrier)
        .collect { case Right(SubscriptionSuccess(id)) => id }
        .recoverWith { case _ => error("Successful response not received from submission") }
      _ <- subscriptionDetailsService.saveSubscriptionId(mtditid)
        .recoverWith { case _ => error("Failed to save to Subscription Details ") }
    } yield Redirect(controllers.agent.routes.ConfirmationController.show()).addingToSession(ITSASessionKeys.MTDITID -> mtditid)
  }

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      implicit cache =>
        val arn: String = user.arn.getOrElse(
          throw new InternalServerException("[CheckYourAnswers][submit] - ARN not found")
        )
        val nino: String = user.clientNino.getOrElse(
          throw new InternalServerException("[CheckYourAnswersController][submit] - Client nino not found")
        )
        val utr: String = user.clientUtr.getOrElse(
          throw new InternalServerException("[CheckYourAnswersController][submit] - Client utr not found")
        )
        if (isEnabled(ReleaseFour)) {
          submitForAuthorisedAgentWithReleaseFourEnabled(arn = arn, nino = nino, utr = utr)
        } else {
          submitForAuthorisedAgent(arn = arn, nino = nino, utr = utr)
        }

  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any Subscription Details  cached data")

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }

}
