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
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.AgentAnswers._
import controllers.utils.{ReferenceRetrieval, RequireAnswer}
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
import views.html.agent.CheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           subscriptionService: SubscriptionOrchestrationService,
                                           incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           implicitDateFormatter: ImplicitDateFormatterImpl,
                                           checkYourAnswers: CheckYourAnswers)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends AuthenticatedController
  with FeatureSwitching
  with RequireAnswer
  with ReferenceRetrieval {

  private def journeySafeGuard(processFunc: => IncomeTaxAgentUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        withAgentReference { reference =>
          if (user.clientNino.isDefined && user.clientUtr.isDefined) {
            subscriptionDetailsService.fetchAll(reference).flatMap {
              case cache => processFunc(user)(request)(cache)
              case _ => error(noCacheMapErrMessage)
            }
          } else {
            Future.successful(Redirect(controllers.agent.matching.routes.ConfirmClientController.show()))
          }
        }
    }

  private def getSelfEmploymentsData(reference: String)
                                    (implicit request: Request[AnyContent]): Future[(Option[Seq[SelfEmploymentData]], Option[AccountingMethodModel])] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
    } yield (businesses, businessAccountingMethod)
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        withAgentReference { reference =>
          require(reference)(incomeSourceModelAnswer) { incomeSource =>
            for {
              (businesses, businessAccountingMethod) <- getSelfEmploymentsData(reference)
              property <- subscriptionDetailsService.fetchProperty(reference)
              overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
            } yield {
              Ok(checkYourAnswers(
                cache.getAgentSummary(businesses, businessAccountingMethod, property, overseasProperty, isReleaseFourEnabled = true),
                controllers.agent.routes.CheckYourAnswersController.submit(),
                backUrl = backUrl(incomeSource),
                implicitDateFormatter,
                releaseFour = true
              ))
            }
          }
        }
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any Subscription Details  cached data")

  private def submitForAuthorisedAgent(reference: String, arn: String, nino: String, utr: String)
                                      (implicit user: IncomeTaxAgentUser, request: Request[AnyContent],
                                       cache: CacheMap): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    for {
      (businesses, businessAccountingMethod) <- getSelfEmploymentsData(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      mtditid <- subscriptionService.createSubscription(
        arn = arn,
        nino = nino,
        utr = utr,
        summaryModel = cache.getAgentSummary(businesses, businessAccountingMethod, property, overseasProperty, isReleaseFourEnabled = true),
        isReleaseFourEnabled = true
      )(headerCarrier)
        .collect { case Right(SubscriptionSuccess(id)) => id }
        .recoverWith { case _ => error("Successful response not received from submission") }
      _ <- subscriptionDetailsService.saveSubscriptionId(reference, mtditid)
        .recoverWith { case _ => error("Failed to save to Subscription Details ") }
    } yield Redirect(controllers.agent.routes.ConfirmationAgentController.show()).addingToSession(ITSASessionKeys.MTDITID -> mtditid)
  }

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      implicit cache =>
        withAgentReference { reference =>
          val arn: String = user.arn.getOrElse(
            throw new InternalServerException("[CheckYourAnswers][submit] - ARN not found")
          )
          val nino: String = user.clientNino.getOrElse(
            throw new InternalServerException("[CheckYourAnswersController][submit] - Client nino not found")
          )
          val utr: String = user.clientUtr.getOrElse(
            throw new InternalServerException("[CheckYourAnswersController][submit] - Client utr not found")
          )
          submitForAuthorisedAgent(reference = reference, arn = arn, nino = nino, utr = utr)
        }
  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any Subscription Details  cached data")

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }

  def backUrl(incomeSource: IncomeSourceModel): String = {
    incomeSource match {
      case IncomeSourceModel(_, _, true) =>
        controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show().url
      case IncomeSourceModel(_, true, _) =>
        controllers.agent.business.routes.PropertyAccountingMethodController.show().url
      case _ =>
        appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-accounting-method"
    }
  }
}
