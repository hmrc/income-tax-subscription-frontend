/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.{AuthenticatedController, IncomeTaxSAUser}
import config.BaseControllerConfig
import connectors.models.subscription.SubscriptionSuccess
import controllers.ITSASessionKeys._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{AuthService, CitizenDetailsService, KeystoreService, SubscriptionService}
import uk.gov.hmrc.play.http.InternalServerException
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               subscriptionService: SubscriptionService,
                               keystoreService: KeystoreService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               logging: Logging
                              ) extends AuthenticatedController {

  lazy val showGuidance: Boolean = baseConfig.applicationConfig.showGuidance

  def home: Action[AnyContent] = Action { implicit request =>
    val redirect = controllers.routes.HomeController.index()

    showGuidance match {
      case true =>
        Ok(views.html.frontpage(redirect))
      case _ =>
        Redirect(redirect)
    }
  }

  private def checkAuth(default: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] = {

    lazy val error = Future.failed(new InternalServerException(s"HomeController.checkAuth: unexpected error calling the citizen details service"))

    (user.nino, user.utr) match {
      case (Some(nino), None) => citizenDetailsService.lookupUtr(nino).flatMap {
        case Right(optUtr) =>
          optUtr match {
            case Some(utr) =>
              Future.successful(Redirect(controllers.routes.IncomeSourceController.showIncomeSource()))
            case _ =>
              // TODO this condition will be changed to redirect to the registration service when it becomes available,
              // but for now the content on the no nino page will suffice
              Future.successful(Redirect(controllers.routes.NoNinoController.showNoNino()))
          }
          Future.failed(new InternalServerException(s"HomeController.index: unexpected error calling the subscription service"))
        case _ =>
          error
      }.recoverWith { case _ => error }
      case _ => default
    }
  }

  private def checkAlreadySubscribed(default: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
    subscriptionService.getSubscription(user.nino.get).flatMap {
      case Right(None) => default
      case Right(Some(SubscriptionSuccess(mtditId))) =>
        keystoreService.saveSubscriptionId(mtditId) map { _ =>
          Redirect(controllers.routes.ClaimSubscriptionController.claim())
        }
      case _ =>
        Future.failed(new InternalServerException(s"HomeController.index: unexpected error calling the subscription service"))
    }

  def index: Action[AnyContent] = Authenticated.asyncForHomeController { implicit request =>
    implicit user =>
      val timestamp: String = java.time.LocalDateTime.now().toString
      checkAuth(checkAlreadySubscribed(gotoPreferences.addingToSession(StartTime -> timestamp)))
  }

  lazy val gotoPreferences = Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
}
