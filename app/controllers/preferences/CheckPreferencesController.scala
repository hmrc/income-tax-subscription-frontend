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

import config.{BaseControllerConfig, YtaHeaderCarrierForPartialsConverter}
import YtaHeaderCarrierForPartialsConverter._
import connectors.preferences.PreferenceFrontendConnector
import controllers.BaseController
import play.api.i18n.MessagesApi

import scala.concurrent.ExecutionContext.Implicits.global

class CheckPreferencesController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val preferenceConnector: PreferenceFrontendConnector) extends BaseController {

  def checkPreference = Authorised.async { implicit user =>
    implicit request =>
      preferenceConnector.checkPaperless.map {
        case x => Ok(x.toString+" "+preferenceConnector.checkPaperlessUrl)
      }
  }

}
