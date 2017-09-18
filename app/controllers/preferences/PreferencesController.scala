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

package controllers.preferences

import javax.inject.{Inject, Singleton}

import auth.AuthenticatedController
import config.BaseControllerConfig
import connectors.models.preferences.{Activated, Unset}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.{AuthService, KeystoreService, PaperlessPreferenceTokenService, PreferencesService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val preferencesService: PreferencesService,
                                      val authService: AuthService,
                                      keystoreService: KeystoreService,
                                      paperlessPreferenceTokenService: PaperlessPreferenceTokenService
                                     ) extends AuthenticatedController {

  def view()(implicit request: Request[AnyContent]): Html = {
    views.html.preferences.continue_registration(
      postAction = controllers.preferences.routes.PreferencesController.submitGoBackToPreferences()
    )
  }

  def checkPreferences: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        token <- paperlessPreferenceTokenService.storeNino(user.nino.get)
        res <- if (baseConfig.applicationConfig.userMatchingFeature) {
          Future.successful(Redirect(controllers.routes.IncomeSourceController.showIncomeSource()))
        } else {
          preferencesService.checkPaperless(token).map {
            case Right(Activated) => Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
            case Right(Unset(Some(url))) => Redirect(url)
            //TODO Remove after feature switch is removed as redirect url will become non-optional
            case Right(Unset(None)) => Redirect(preferencesService.defaultChoosePaperlessUrl)
            case _ => throw new InternalServerException("Could not get paperless preferences")
          }
        }
      } yield res
  }

  def callback: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      paperlessPreferenceTokenService.storeNino(user.nino.get) flatMap {
        token =>
          preferencesService.checkPaperless(token).map {
            case Right(Activated) => Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
            case Right(_) => Redirect(controllers.preferences.routes.PreferencesController.showGoBackToPreferences())
            case _ => throw new InternalServerException("Could not get paperless preferences")
          }
      }
  }

  def showGoBackToPreferences: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      if (baseConfig.applicationConfig.userMatchingFeature) Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
      else Ok(view())
  }

  def submitGoBackToPreferences: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      if (baseConfig.applicationConfig.userMatchingFeature) Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
      else gotoPreferences
  }

  @inline def gotoPreferences(implicit request: Request[AnyContent]): Result = Redirect(preferencesService.defaultChoosePaperlessUrl)

  def signOut(implicit request: Request[_]): Result = Redirect(controllers.routes.SignOutController.signOut())

}
