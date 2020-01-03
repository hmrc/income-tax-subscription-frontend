/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.usermatching

import core.auth.{IncomeTaxSAUser, UserMatchingController}
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import usermatching.forms.UserDetailsForm
import usermatching.models.{NotLockedOut, UserDetailsModel}
import usermatching.services.UserLockoutService

import scala.concurrent.Future

@Singleton
class UserDetailsController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val authService: AuthService,
                                      val lockOutService: UserLockoutService
                                     ) extends UserMatchingController {

  def view(userDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    usermatching.views.html.user_details(
      userDetailsForm,
      controllers.usermatching.routes.UserDetailsController.submit(editMode = isEditMode),
      isEditMode
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]) = {
    val bearerToken = implicitly[HeaderCarrier].userId.get.value

    (lockOutService.getLockoutStatus(bearerToken) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(controllers.usermatching.routes.UserDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("user details controller: " + e)
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        Future.successful(Ok(view(UserDetailsForm.userDetailsForm.form.fill(request.fetchUserDetails), isEditMode = isEditMode)))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        UserDetailsForm.userDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
          userDetails => {
            val continue = Redirect(controllers.usermatching.routes.ConfirmUserController.show()).saveUserDetails(userDetails)

            if (request.fetchUserDetails.fold(false)(_ != userDetails)) keystoreService.deleteAll().map(_ => continue)
            else Future.successful(continue)
          }
        )
      }
  }

}
