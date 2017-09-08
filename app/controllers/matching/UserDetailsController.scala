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

package controllers.matching

import javax.inject.{Inject, Singleton}

import auth.{AuthenticatedController, IncomeTaxSAUser}
import config.BaseControllerConfig
import connectors.models.matching.NotLockedOut
import forms.UserDetailsForm
import models.matching.UserDetailsModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.{AuthService, KeystoreService, UserLockoutService}
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class UserDetailsController @Inject()(val baseConfig: BaseControllerConfig,
                                        val messagesApi: MessagesApi,
                                        val keystoreService: KeystoreService,
                                        val authService: AuthService,
                                        val lockOutService: UserLockoutService
                                       ) extends AuthenticatedController {

  def view(userDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.user_details(
      userDetailsForm,
      controllers.matching.routes.UserDetailsController.submit(editMode = isEditMode),
      isEditMode
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]) = {
    val bearerToken = implicitly[HeaderCarrier].userId.get

    println(bearerToken)

    (lockOutService.getLockoutStatus(bearerToken) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(controllers.matching.routes.UserDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("client details controller: " + e)
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchUserDetails() map {
          userDetails => Ok(view(UserDetailsForm.userDetailsForm.form.fill(userDetails), isEditMode = isEditMode))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        UserDetailsForm.userDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
          userDetails => {
            val persist = keystoreService.fetchUserDetails().flatMap {
              case Some(oldDetails) if oldDetails == userDetails =>
                Future.successful(Unit)
              case Some(_) =>
                // n.b. the delete must come before the save otherwise nothing will ever be saved.
                // this feature is currently NOT unit testable
                for {
                  _ <- keystoreService.deleteAll()
                  _ <- keystoreService.saveUserDetails(userDetails)
                } yield Unit
              case None =>
                keystoreService.saveUserDetails(userDetails)
            }

            for {
              _ <- persist
            } yield Redirect(routes.ConfirmUserController.show())
          }
        )
      }
  }

}
