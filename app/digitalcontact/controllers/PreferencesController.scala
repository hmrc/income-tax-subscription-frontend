/*
 * Copyright 2018 HM Revenue & Customs
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

package digitalcontact.controllers

import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys
import core.auth.StatelessController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import digitalcontact.models.{Activated, Unset}
import digitalcontact.services.{PaperlessPreferenceTokenService, PreferencesService}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException
import usermatching.userjourneys.ConfirmAgentSubscription

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val preferencesService: PreferencesService,
                                      val authService: AuthService,
                                      keystoreService: KeystoreService,
                                      paperlessPreferenceTokenService: PaperlessPreferenceTokenService
                                     ) extends StatelessController {

  override val statelessDefaultPredicate = preferencesPredicate

  def view()(implicit request: Request[AnyContent]): Html = {
    digitalcontact.views.html.continue_registration(
      postAction = digitalcontact.controllers.routes.PreferencesController.submitGoBackToPreferences()
    )
  }

  private def goToNext(implicit request: Request[AnyContent]) =
    if (request.isInState(ConfirmAgentSubscription))
      Redirect(incometax.unauthorisedagent.controllers.routes.UnauthorisedSubscriptionController.subscribeUnauthorised())
    else {
      Redirect(incometax.incomesource.controllers.routes.RentUkPropertyController.show())
    }


  def checkPreferences: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        token <- paperlessPreferenceTokenService.storeNino(user.nino.get)
        res <- preferencesService.checkPaperless(token)
      } yield res match {
        case Right(Activated) => goToNext
        case Right(Unset(url)) => Redirect(url)
        case _ => throw new InternalServerException("Could not get paperless preferences")
      }
  }

  def callback: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      paperlessPreferenceTokenService.storeNino(user.nino.get) flatMap {
        token =>
          preferencesService.checkPaperless(token).map {
            case Right(Activated) => goToNext
            case Right(Unset(url)) => Redirect(digitalcontact.controllers.routes.PreferencesController.showGoBackToPreferences())
              .addingToSession(ITSASessionKeys.PreferencesRedirectUrl -> url)
            case _ => throw new InternalServerException("Could not get paperless preferences")
          }
      }
  }

  def showGoBackToPreferences: Action[AnyContent] = Authenticated { implicit request =>
    implicit user => Ok(view())
  }

  def submitGoBackToPreferences: Action[AnyContent] = Authenticated { implicit request =>
    implicit user => gotoPreferences
  }

  def gotoPreferences(implicit request: Request[AnyContent]): Result =
    request.session.get(ITSASessionKeys.PreferencesRedirectUrl) match {
      case Some(redirectUrl) => Redirect(redirectUrl)
        .removingFromSession(ITSASessionKeys.PreferencesRedirectUrl)
      case None => throw new InternalServerException("No preferences redirect URL provided")
    }

}
