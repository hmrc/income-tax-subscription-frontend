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

import config.BaseControllerConfig
import connectors.models.preferences.Activated
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.PreferencesService
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val preferencesService: PreferencesService) extends BaseController {

  def view()(implicit request: Request[AnyContent]): Html = {
    views.html.preferences.continue_registration(
      postAction = controllers.preferences.routes.PreferencesController.submitGoBackToPreferences()
    )
  }

  def checkPreferences: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      preferencesService.checkPaperless.map {
        case Activated => Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
        case _ => gotoPreferences
      }
  }

  def callback: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      preferencesService.checkPaperless.map {
        case Activated => Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
        case _ => Redirect(controllers.preferences.routes.PreferencesController.showGoBackToPreferences())
      }
  }

  def showGoBackToPreferences: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request => Ok(view())
  }

  def submitGoBackToPreferences: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request => gotoPreferences
  }

  @inline def gotoPreferences(implicit request: Request[AnyContent]): Result = Redirect(preferencesService.choosePaperlessUrl)

  def signOut(implicit request: Request[_]): Result = Redirect(controllers.routes.SignOutController.signOut())

}
