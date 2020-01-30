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

package controllers.individual

import core.ITSASessionKeys
import core.auth.StatelessController
import core.config.BaseControllerConfig
import core.services.AuthService
import digitalcontact.models.{Activated, Unset}
import digitalcontact.services.{PaperlessPreferenceTokenService, PreferencesService}
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val preferencesService: PreferencesService,
                                      val authService: AuthService,
                                      paperlessPreferenceTokenService: PaperlessPreferenceTokenService
                                     ) extends StatelessController {

  override val statelessDefaultPredicate = preferencesPredicate

  def view()(implicit request: Request[AnyContent]): Html = {
    digitalcontact.views.html.continue_registration(
      postAction = controllers.individual.routes.PreferencesController.submit()
    )
  }

  def checkPreferences: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        token <- paperlessPreferenceTokenService.storeNino(user.nino.get)
        res <- preferencesService.checkPaperless(token)
      } yield res match {
        case Right(Activated) => Redirect(controllers.individual.incomesource.routes.RentUkPropertyController.show())
        case Right(Unset(url)) => Redirect(url)
        case _ => throw new InternalServerException("Could not get paperless preferences")
      }
  }

  def callback: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      paperlessPreferenceTokenService.storeNino(user.nino.get) flatMap {
        token =>
          preferencesService.checkPaperless(token).map {
            case Right(Activated) => Redirect(controllers.individual.incomesource.routes.RentUkPropertyController.show())
            case Right(Unset(url)) => Redirect(controllers.individual.routes.PreferencesController.show())
              .addingToSession(ITSASessionKeys.PreferencesRedirectUrl -> url)
            case _ => throw new InternalServerException("Could not get paperless preferences")
          }
      }
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user => Ok(view())
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user => gotoPreferences
  }

  def gotoPreferences(implicit request: Request[AnyContent]): Result =
    request.session.get(ITSASessionKeys.PreferencesRedirectUrl) match {
      case Some(redirectUrl) => Redirect(redirectUrl)
        .removingFromSession(ITSASessionKeys.PreferencesRedirectUrl)
      case None => throw new InternalServerException("No preferences redirect URL provided")
    }

}