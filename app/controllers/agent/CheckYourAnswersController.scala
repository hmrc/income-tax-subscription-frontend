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


import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.individual.subscription._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.AuthService
import services.agent.{KeystoreService, SubscriptionOrchestrationService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.agent.CacheUtil._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authService: AuthService,
                                           val messagesApi: MessagesApi,
                                           keystoreService: KeystoreService,
                                           subscriptionService: SubscriptionOrchestrationService)
                                          (implicit val ec: ExecutionContext, appConfig: AppConfig) extends AuthenticatedController {


  private def journeySafeGuard(processFunc: => IncomeTaxAgentUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (user.clientNino.isDefined && user.clientUtr.isDefined) {
          keystoreService.fetchAll().flatMap {
            case Some(cache) => processFunc(user)(request)(cache)
            case _ => error(noCacheMapErrMessage)
          }
        } else {
          Future.successful(Redirect(controllers.agent.matching.routes.ConfirmClientController.show()))
        }
    }

  def backUrl(incomeSource: Option[IncomeSourceType])(implicit request: Request[_]): String = {
    incomeSource match {
      case Some(Business) =>
        controllers.agent.business.routes.BusinessAccountingMethodController.show().url
      case Some(_) =>
        controllers.agent.business.routes.PropertyAccountingMethodController.show().url
      case None => throw new InternalServerException("User is missing income source type in keystore")
    }
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        for {
          incomeSource <- keystoreService.fetchIncomeSource()
          backLinkUrl = backUrl(incomeSource)
        } yield
          Ok(views.html.agent.check_your_answers(
            cache.getSummary,
            controllers.agent.routes.CheckYourAnswersController.submit(),
            backUrl = backLinkUrl
          ))
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  private def submitForAuthorisedAgent(arn: String, nino: String, utr: String)
                                      (implicit user: IncomeTaxAgentUser, request: Request[AnyContent], cache: CacheMap): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    for {
      mtditid <- subscriptionService.createSubscription(arn = arn, nino = nino, utr = utr, summaryModel = cache.getSummary())(headerCarrier)
        .collect { case Right(SubscriptionSuccess(id)) => id }
        .recoverWith { case _ => error("Successful response not received from submission") }
      _ <- keystoreService.saveSubscriptionId(mtditid)
        .recoverWith { case _ => error("Failed to save to keystore") }
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
        submitForAuthorisedAgent(arn = arn, nino = nino, utr = utr)

  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }

}
