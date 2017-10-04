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

import auth.StatelessController
import config.BaseControllerConfig
import connectors.models.preferences.Activated
import connectors.preferences.PreferenceFrontendConnector
import play.api.i18n.MessagesApi
import services.AuthService
import testonly.connectors.ClearPreferencesConnector
import uk.gov.hmrc.http.{HttpGet, InternalServerException}
import utils.Implicits._

@Singleton
class ClearPreferencesController @Inject()(preferenceFrontendConnector: PreferenceFrontendConnector,
                                           clearPreferencesConnector: ClearPreferencesConnector,
                                           val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           httpGet: HttpGet,
                                           val authService: AuthService
                                          ) extends StatelessController {

  //N.b. asyncUnrestricted is used because it doesn't check any predicates
  val clear = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      user.nino match {
        case None => throw new InternalServerException("clear preferences controller, no nino")
        case Some(nino) =>
          //TODO Update for the new API
          preferenceFrontendConnector.checkPaperless("").flatMap {
            case Right(Activated) =>
              clearPreferencesConnector.clear(nino).map { response =>
                response.status match {
                  case OK => Ok("Preferences cleared")
                  case _ => throw new InternalServerException("Unexpected error in clear pref: status=" + response.status + ", body=" + response.body)
                }
              }
            case _ => Ok("No Preferences found")
          }
      }
  }

}
