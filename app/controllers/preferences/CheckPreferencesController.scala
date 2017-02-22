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

import auth.IncomeTaxSAUser
import config.BaseControllerConfig
import config.YtaHeaderCarrierForPartialsConverter._
import connectors.models.preferences.Activated
import connectors.preferences.PreferenceFrontendConnector
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global

class CheckPreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val preferenceConnector: PreferenceFrontendConnector) extends BaseController {

  def checkPreference = Authorised.async { implicit user =>
    implicit request =>
      preferenceConnector.checkPaperless.map {
        case Activated => Ok(Activated.toString)
        case _ => gotoPreference
      }
  }

  def gotoPreference(implicit user: IncomeTaxSAUser, request: Request[AnyContent]) = Redirect(preferenceConnector.choosePaperlessUrl)


}
