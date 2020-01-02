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

package controllers.agent

import agent.audit.Logging
import agent.auth.AgentJourneyState._
import agent.auth.{AuthenticatedController, IncomeTaxAgentUser}
import agent.common.Constants
import agent.services.{ClientRelationshipService, KeystoreService, SubscriptionOrchestrationService}
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.services.AuthService
import incometax.subscription.models.{Both, Business, IncomeSourceType, SubscriptionSuccess}
import incometax.unauthorisedagent.services.SubscriptionStorePersistenceService
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val subscriptionService: SubscriptionOrchestrationService,
                                           val clientRelationshipService: ClientRelationshipService,
                                           val subscriptionStorePersistenceService: SubscriptionStorePersistenceService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends AuthenticatedController with FeatureSwitching {

  import agent.services.CacheUtil._

  private def journeySafeGuard(processFunc: => IncomeTaxAgentUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String) =
    Authenticated.async { implicit request =>
      implicit user =>
        if (user.clientNino.isDefined) {
          keystoreService.fetchAll().flatMap {
            case Some(cache) =>
              if (isEnabled(EligibilityPagesFeature))
                processFunc(user)(request)(cache)
              else {
                cache.getTerms() match {
                  case Some(true) => processFunc(user)(request)(cache)
                  case Some(false) => Future.successful(Redirect(controllers.agent.routes.TermsController.show(editMode = true)))
                  case _ => Future.successful(Redirect(controllers.agent.routes.TermsController.show()))
                }
              }
            case _ => error(noCacheMapErrMessage)
          }
        } else {
          Future.successful(Redirect(controllers.agent.matching.routes.ConfirmClientController.show()))
        }
    }

  def backUrl(incomeSource: Option[IncomeSourceType])(implicit request: Request[_]): String = {
    if (isEnabled(EligibilityPagesFeature)) {
      if (isEnabled(AgentPropertyCashOrAccruals)) {
        incomeSource match {
          case Some(Business) =>
            controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          case Some(_) =>
            controllers.agent.business.routes.PropertyAccountingMethodController.show().url
          case None => throw new InternalServerException("User is missing income source type in keystore")
        }
      } else {
        incomeSource match {
          case Some(Business) | Some(Both) =>
            controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          case Some(_) =>
            controllers.agent.routes.IncomeSourceController.show().url
          case None => throw new InternalServerException("User is missing income source type in keystore")
        }
      }
    } else {
      controllers.agent.routes.TermsController.show().url
    }
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        for {
          incomeSource <- keystoreService.fetchIncomeSource()
          backLinkUrl = backUrl(incomeSource)
        } yield
          Ok(agent.views.html.check_your_answers(
            cache.getSummary,
            controllers.agent.routes.CheckYourAnswersController.submit(),
            backUrl = backLinkUrl
          ))
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  private def submitForAuthorisedAgent(arn: String, nino: String
                                      )(implicit user: IncomeTaxAgentUser, request: Request[AnyContent], cache: CacheMap
                                      ): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    for {
      mtditid <- subscriptionService.createSubscription(arn, nino, cache.getSummary())(headerCarrier)
        .collect { case Right(SubscriptionSuccess(id)) => id }
        .recoverWith { case _ => error("Successful response not received from submission") }
      cacheMap <- keystoreService.saveSubscriptionId(mtditid)
        .recoverWith { case _ => error("Failed to save to keystore") }
    } yield Redirect(controllers.agent.routes.ConfirmationController.show()).addingToSession(ITSASessionKeys.MTDITID -> mtditid)
  }

  private def submitForUnauthorisedAgent(arn: String, nino: String
                                        )(implicit user: IncomeTaxAgentUser, request: Request[AnyContent], cache: CacheMap
                                        ): Future[Result] =
    subscriptionStorePersistenceService.storeSubscription(arn, nino) flatMap {
      case Right(_) =>
        Future.successful(
          Redirect(controllers.agent.routes.UnauthorisedAgentConfirmationController.show())
            // n.b. we're only using this flag to safeguard the reset of the journey so that the user can't go back to them
            .addingToSession(ITSASessionKeys.MTDITID -> Constants.unauthorisedAgentMtdValue)
        )
      case Left(err) => error("Error calling income-tax-subscription-store: " + err)
    }

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      implicit cache =>
        // Will fail if there is no NINO
        val nino = user.clientNino.get
        // Will fail if there is no ARN in session
        val arn = user.arn.get

        if (request.isUnauthorisedAgent) submitForUnauthorisedAgent(arn, nino)
        else submitForAuthorisedAgent(arn, nino)

  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }

}
