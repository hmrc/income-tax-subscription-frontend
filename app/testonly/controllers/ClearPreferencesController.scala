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

import core.auth.StatelessController
import core.config.BaseControllerConfig
import core.services.AuthService
import digitalcontact.connectors.PreferenceFrontendConnector
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Request, Result}
import play.twirl.api.Html
import testonly.connectors.ClearPreferencesConnector
import testonly.forms.ClearPreferencesForm
import testonly.models.preferences.{ClearPreferencesModel, ClearPreferencesResult, Cleared, NoPreferences}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, InternalServerException}

import scala.concurrent.Future

@Singleton
class ClearPreferencesController @Inject()(preferenceFrontendConnector: PreferenceFrontendConnector,
                                           clearPreferencesConnector: ClearPreferencesConnector,
                                           val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           httpGet: HttpGet,
                                           val authService: AuthService
                                          ) extends StatelessController {

  private def clearUser(nino: String)(implicit hc: HeaderCarrier): Future[ClearPreferencesResult] = clearPreferencesConnector.clear(nino).map { response =>
    response.status match {
      case OK => Cleared(nino)
      case NO_CONTENT => NoPreferences(nino)
      case _ => throw new InternalServerException("Unexpected error in clear pref: status=" + response.status + ", body=" + response.body)
    }
  }

  val clear = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      user.nino match {
        case None => Future.failed[Result](new InternalServerException("clear preferences controller, no nino"))
        case Some(nino) => clearUser(nino) map {
          case Cleared(_) => Ok("Preferences cleared")
          case NoPreferences(_) => Ok("No preferences found")
        }
      }
  }


  private def showView(form: Form[ClearPreferencesModel])(implicit request: Request[_]): Html =
    testonly.views.html.clear_preferences(
      clearPreferencesForm = form,
      postAction = testonly.controllers.routes.ClearPreferencesController.submitClearUser()
    )

  val showClearUser = Action.async { implicit request =>
    Future.successful(Ok(showView(ClearPreferencesForm.ClearPreferenceValidationForm)))
  }

  val submitClearUser = Action.async { implicit request =>
    ClearPreferencesForm.ClearPreferenceForm.bindFromRequest().fold(
      badRequest => Future.successful(BadRequest(showView(badRequest))),
      clearPreferences =>
        clearUser(clearPreferences.nino) map {
          case Cleared(nino) => Ok(s"Preferences cleared for $nino")
          case NoPreferences(nino) => Ok(s"No preferences found for $nino")
        }
    )
  }

}
