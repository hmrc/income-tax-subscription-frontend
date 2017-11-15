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

package agent.controllers.matching

import javax.inject.{Inject, Singleton}

import agent.auth.{IncomeTaxAgentUser, UserMatchingController}
import agent.forms._
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException
import usermatching.models.{NotLockedOut, UserDetailsModel}
import usermatching.services.UserLockoutService

import scala.concurrent.Future

@Singleton
class ClientDetailsController @Inject()(val baseConfig: BaseControllerConfig,
                                        val messagesApi: MessagesApi,
                                        val keystoreService: KeystoreService,
                                        val authService: AuthService,
                                        val lockOutService: UserLockoutService
                                       ) extends UserMatchingController {

  def view(clientDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    agent.views.html.client_details(
      clientDetailsForm,
      agent.controllers.matching.routes.ClientDetailsController.submit(editMode = isEditMode),
      isEditMode
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]) = {
    (lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("client details controller: " + e)
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchClientDetails() map {
          clientDetails => Ok(view(ClientDetailsForm.clientDetailsForm.form.fill(clientDetails), isEditMode = isEditMode))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        ClientDetailsForm.clientDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
          clientDetails => {
            val persist = keystoreService.fetchClientDetails().flatMap {
              case Some(oldDetails) if oldDetails == clientDetails =>
                Future.successful(Unit)
              case Some(_) =>
                // n.b. the delete must come before the save otherwise nothing will ever be saved.
                // this feature is currently NOT unit testable
                for {
                  _ <- keystoreService.deleteAll()
                  _ <- keystoreService.saveClientDetails(clientDetails)
                } yield Unit
              case None =>
                keystoreService.saveClientDetails(clientDetails)
            }

            for {
              _ <- persist
            } yield Redirect(routes.ConfirmClientController.show())
          }
        )
      }
  }

}
