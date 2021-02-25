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

package controllers.agent.matching

import auth.agent.{IncomeTaxAgentUser, UserMatchingController}
import config.AppConfig
import forms.agent.ClientDetailsForm
import javax.inject.{Inject, Singleton}
import models.usermatching.{NotLockedOut, UserDetailsModel}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService, UserLockoutService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsController @Inject()(val auditingService: AuditingService,
                                        val authService: AuthService,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        lockOutService: UserLockoutService)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents,
                                        val appConfig: AppConfig) extends UserMatchingController {

  def view(clientDetailsForm: Form[UserDetailsModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.agent.client_details(
      clientDetailsForm,
      controllers.agent.matching.routes.ClientDetailsController.submit(editMode = isEditMode),
      isEditMode
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]): Future[Result] = {
    lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show().url))
      case Left(_) => throw new InternalServerException("[ClientDetailsController][handleLockOut] lockout failure")
    }
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        Future.successful(Ok(view(ClientDetailsForm.clientDetailsForm.form.fill(request.fetchUserDetails), isEditMode = isEditMode)))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        ClientDetailsForm.clientDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
          clientDetails => {
            val continue = Redirect(routes.ConfirmClientController.show()).saveUserDetails(clientDetails)

            if (request.fetchUserDetails.fold(false)(_ != clientDetails)) subscriptionDetailsService.deleteAll().map(_ => continue)
            else Future.successful(continue)
          }
        )
      }
  }

}
