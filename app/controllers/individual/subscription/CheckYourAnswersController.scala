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
import javax.inject.{Inject, Singleton}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result, _}
import services.individual.SubscriptionOrchestrationService
import services.{AuthService, KeystoreService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.CacheUtil._
import utilities.ITSASessionKeys
import utilities.individual.{ImplicitDateFormatterImpl}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authService: AuthService,
                                           keystoreService: KeystoreService,
                                           subscriptionService: SubscriptionOrchestrationService,
                                           implicitDateFormatter: ImplicitDateFormatterImpl
                                          )
                                          (implicit val ec: ExecutionContext,
                                           appConfig: AppConfig,
                                           mcc: MessagesControllerComponents
                                          ) extends SignUpController {


  def backUrl(incomeSource: IncomeSourceModel): String = {
    incomeSource match {
      case IncomeSourceModel(_, true, _) =>
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
      case IncomeSourceModel(true, false, _) =>
        controllers.individual.business.routes.BusinessAccountingMethodController.show().url
    }
  }

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        Future.successful(
          Ok(views.html.individual.incometax.subscription.check_your_answers(
            cache.getSummary(),
            controllers.individual.subscription.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl(cache.getIncomeSourceModel.get),
            implicitDateFormatter
          ))
        )
  }

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
        subscriptionService.createSubscription(nino, cache.getSummary())(headerCarrier).flatMap {
          case Right(SubscriptionSuccess(id)) =>
            keystoreService.saveSubscriptionId(id).map(_ => Redirect(controllers.individual.subscription.routes.ConfirmationController.show()))
          case Left(failure) =>
            error("Successful response not received from submission: \n" + failure.toString)
        }
  }

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result]): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        keystoreService.fetchAll().flatMap { cache =>
          processFunc(user)(request)(cache)
        }
    }

  def error(message: String): Future[Nothing] = {
    Logger.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
