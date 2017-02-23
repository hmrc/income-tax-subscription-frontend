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

import javax.inject.Inject

import config.BaseControllerConfig
import connectors.models.preferences.Activated
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.PreferencesService

import scala.concurrent.ExecutionContext.Implicits.global

class PreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val preferencesService: PreferencesService) extends BaseController {

  def checkPreference: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      preferencesService.checkPaperless.map {
        case Activated => Ok(Activated.toString)
        case _ => gotoPreference
      }
  }

  @inline def gotoPreference(implicit request: Request[AnyContent]): Result = Redirect(preferencesService.choosePaperlessUrl)

}
