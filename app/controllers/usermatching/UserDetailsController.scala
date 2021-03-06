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

package controllers.usermatching

import auth.individual.{IncomeTaxSAUser, UserMatchingController}
import config.AppConfig
import forms.usermatching.UserDetailsForm
import models.usermatching.{NotLockedOut, UserDetailsModel}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService, UserLockoutService}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDetailsController @Inject()(val auditingService: AuditingService,
                                      val authService: AuthService,
                                      subscriptionDetailsService: SubscriptionDetailsService,
                                      lockOutService: UserLockoutService)
                                     (implicit val ec: ExecutionContext,
                                      val appConfig: AppConfig,
                                      mcc: MessagesControllerComponents) extends UserMatchingController {

  def view(userDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.individual.usermatching.user_details(
      userDetailsForm,
      controllers.usermatching.routes.UserDetailsController.submit(editMode = isEditMode),
      isEditMode

    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]): Future[Result] = {

    lockOutService.getLockoutStatus(user.userId) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(controllers.usermatching.routes.UserDetailsLockoutController.show().url))
      case Left(_) => throw new InternalServerException("[UserDetailsController][handleLockOut] failure response from lockout service")
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        Future.successful(Ok(view(
          UserDetailsForm.userDetailsForm.form.fill(request.fetchUserDetails),
          isEditMode = isEditMode)))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        UserDetailsForm.userDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(
            formWithErrors,
            isEditMode = isEditMode))),
          userDetails => {
            val continue = Redirect(controllers.usermatching.routes.ConfirmUserController.show()).saveUserDetails(userDetails)

            if (request.fetchUserDetails.fold(false)(_ != userDetails)) subscriptionDetailsService.deleteAll().map(_ => continue)
            else Future.successful(continue)
          }
        )
      }
  }
}
