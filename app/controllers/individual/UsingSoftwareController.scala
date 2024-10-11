/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual

import auth.individual.SignUpController
import config.AppConfig
import forms.individual.UsingSoftwareForm
import models.YesNo
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, GetEligibilityStatusService, SessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.UsingSoftware
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext



@Singleton
class UsingSoftwareController @Inject()(usingSoftware: UsingSoftware,
                                        sessionDataService: SessionDataService,
                                        eligibilityStatusService: GetEligibilityStatusService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends SignUpController {

  private val form: Form[YesNo] = UsingSoftwareForm.usingSoftwareForm


  def view(usingSoftwareForm: Form[YesNo],
           eligibleNextYearOnly: Boolean)
          (implicit request: Request[_]): Html = {
    usingSoftware(
      usingSoftwareForm = usingSoftwareForm,
      postAction = controllers.individual.routes.UsingSoftwareController.submit()
    )
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        usingSoftwareStatus <- sessionDataService.fetchSoftwareStatus
        eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
      } yield {
        usingSoftwareStatus match {
          case Left(_) => throw new InternalServerException("[UsingSoftwareController][show] - Could not fetch software status")

          case Right(maybeYesNo) =>
            Ok(view(
              usingSoftwareForm = form.fill(maybeYesNo),
              eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly
            ))
        }

      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        formWithErrors =>
          eligibilityStatusService.getEligibilityStatus map { eligibility =>
            BadRequest(
              view(
                usingSoftwareForm = formWithErrors,
                eligibleNextYearOnly = eligibility.eligibleNextYearOnly
              )
            )
          }, yesNo =>
          sessionDataService.saveSoftwareStatus(yesNo) map {
            case Left(_) => throw new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer")
            case Right(_) => Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
          }
      )
  }

}