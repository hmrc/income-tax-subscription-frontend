/*
 * Copyright 2021 HM Revenue & Customs
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

package testonly.controllers.individual

import auth.individual.StatelessController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.AuthService
import testonly.connectors.individual.ClearPreferencesConnector
import testonly.form.individual.ClearPreferencesForm
import testonly.models.preferences.{ClearPreferencesModel, ClearPreferencesResult, Cleared, NoPreferences}
import testonly.views.html.individual.clear_preferences
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClearPreferencesController @Inject()(val authService: AuthService, clearPreferencesConnector: ClearPreferencesConnector)
                                          (implicit val ec: ExecutionContext, appConfig: AppConfig, mcc: MessagesControllerComponents) extends StatelessController {

  private def clearUser(nino: String)(implicit hc: HeaderCarrier): Future[ClearPreferencesResult] = clearPreferencesConnector.clear(nino).map { response =>
    response.status match {
      case OK => Cleared(nino)
      case NO_CONTENT => NoPreferences(nino)
      case _ => throw new InternalServerException("Unexpected error in clear pref: status=" + response.status + ", body=" + response.body)
    }
  }

  val clear: Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
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
    clear_preferences(
      clearPreferencesForm = form,
      postAction = testonly.controllers.individual.routes.ClearPreferencesController.submit()
    )

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(showView(ClearPreferencesForm.ClearPreferenceValidationForm)))
  }

  val submit: Action[AnyContent] = Action.async { implicit request =>
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
