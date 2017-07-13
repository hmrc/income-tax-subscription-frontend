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

package testonly.controllers

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import connectors.models.preferences.Activated
import connectors.preferences.PreferenceFrontendConnector
import controllers.AuthenticatedController
import play.api.i18n.MessagesApi
import services.AuthService
import testonly.connectors.ClearPreferencesConnector
import uk.gov.hmrc.play.http.HttpGet
import utils.Implicits._

@Singleton
class ClearPreferencesController @Inject()(preferenceFrontendConnector: PreferenceFrontendConnector,
                                           clearPreferencesConnector: ClearPreferencesConnector,
                                           val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           httpGet: HttpGet,
                                           val authService: AuthService
                                          ) extends AuthenticatedController {

  val clear = Authenticated.async { implicit request =>
    implicit user =>
      user.nino match {
        case None => InternalServerError("no nino")
        case Some(nino) =>
          preferenceFrontendConnector.checkPaperless.flatMap {
            case Activated =>
              clearPreferencesConnector.clear(nino).map { response =>
                response.status match {
                  case OK => Ok("Preferences cleared")
                  case _ => InternalServerError("Unexpected error: status=" + response.status + ", body=" + response.body)
                }
              }
            case _ => Ok("No Preferences found")
          }
      }
  }

}
