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
import controllers.utils.ReferenceRetrieval
import forms.individual.UsingSoftwareForm
import models.YesNo
import play.api.data.Form
import play.api.mvc.*
import play.twirl.api.Html
import services.*
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.UsingSoftware

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UsingSoftwareController @Inject()(usingSoftware: UsingSoftware,
                                        sessionDataService: SessionDataService,
                                        eligibilityStatusService: GetEligibilityStatusService,
                                        mandationStatusService: MandationStatusService,
                                        referenceRetrieval: ReferenceRetrieval,
                                        subscriptionDetailsService: SubscriptionDetailsService
                                       )
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends SignUpController {

  private val form: Form[YesNo] = UsingSoftwareForm.usingSoftwareForm


  def view(usingSoftwareForm: Form[YesNo], editMode: Boolean)
          (implicit request: Request[_]): Html = {
    usingSoftware(
      usingSoftwareForm = usingSoftwareForm,
      postAction = controllers.individual.routes.UsingSoftwareController.submit(editMode)
    )
  }

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        sessionData <- sessionDataService.getAllSessionData()
      } yield {
        val usingSoftwareStatus = sessionData.fetchSoftwareStatus
        Ok(view(
          usingSoftwareForm = form.fill(usingSoftwareStatus),
          editMode = editMode
        ))
      }
  }

  def submit(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful {
            BadRequest(view(
              usingSoftwareForm = formWithErrors,
              editMode = editMode
            ))
          },
        yesNo =>
          for {
            sessionData <- sessionDataService.getAllSessionData()
            usingSoftwareStatus <- sessionDataService.saveSoftwareStatus(yesNo)
            mandationStatus <- mandationStatusService.getMandationStatus(sessionData)
          } yield {
            val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated

            usingSoftwareStatus match {
              case Left(_) =>
                throw new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer")
              case Right(_) =>
                if (editMode) {
                  Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
                } else if (isMandatedCurrentYear) {
                  Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                } else {
                  Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                }
            }
          }
      )
  }
}
