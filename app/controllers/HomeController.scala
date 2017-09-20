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
import auth._
import config.BaseControllerConfig
import connectors.models.CitizenDetailsSuccess
import connectors.models.subscription.SubscriptionSuccess
import controllers.ITSASessionKeys._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{AuthService, CitizenDetailsService, KeystoreService, SubscriptionService}
import utils.Implicits._
import auth.JourneyState._

import scala.concurrent.Future
import uk.gov.hmrc.http.InternalServerException

@Singleton
class HomeController @Inject()(override val baseConfig: BaseControllerConfig,
                               override val messagesApi: MessagesApi,
                               subscriptionService: SubscriptionService,
                               keystoreService: KeystoreService,
                               val authService: AuthService,
                               citizenDetailsService: CitizenDetailsService,
                               logging: Logging
                              ) extends StatelessController {

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

  private def checkCID(defaultAction: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] = {

    lazy val error = Future.failed(new InternalServerException("HomeController.checkCID: unexpected error calling the citizen details service"))

    // TODO this condition will be changed to redirect to the registration service when it becomes available,
    // but for now the content on the no nino page will suffice
    lazy val gotoRegistration = Future.successful(
      Redirect(controllers.routes.NoNinoController.showNoNino())
    )

    (user.nino, user.utr) match {
      case (Some(_), Some(_)) => defaultAction
      case (Some(nino), None) =>
        // todo if nino is in session but not in auth profile then don't call CID
        citizenDetailsService.lookupUtr(nino).flatMap {
          case Right(optResult) =>
            optResult match {
              case Some(CitizenDetailsSuccess(optUtr@Some(utr))) => defaultAction.flatMap(_.addingToSession(ITSASessionKeys.UTR -> utr))
              case Some(CitizenDetailsSuccess(None)) => gotoRegistration
              case _ => error
            }
          case _ => gotoRegistration
        }.recoverWith { case _ => error }
      case (None, _) => // n.b. This should not happen as the user have been redirected by the no nino predicates
        Future.failed(new InternalServerException("HomeController.checkCID: unexpected user state, the user has a utr but no nino"))
    }
  }

  private def checkAlreadySubscribed(default: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[AnyContent]): Future[Result] =
    subscriptionService.getSubscription(user.nino.get).flatMap {
      case Right(None) => default
      case Right(Some(SubscriptionSuccess(mtditId))) =>
        keystoreService.saveSubscriptionId(mtditId) map { _ =>
          Redirect(controllers.routes.ClaimSubscriptionController.claim()).withJourneyState(SignUp)
        }
      case _ =>
        Future.failed(new InternalServerException(s"HomeController.index: unexpected error calling the subscription service"))
    }

  def index: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val timestamp: String = java.time.LocalDateTime.now().toString
      checkCID(
        checkAlreadySubscribed(
          gotoPreferences.addingToSession(StartTime -> timestamp).withJourneyState(SignUp)
        )
      )
  }

  lazy val gotoPreferences = Redirect(controllers.preferences.routes.PreferencesController.checkPreferences())
}
