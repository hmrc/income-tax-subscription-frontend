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
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import forms.individual.UsingSoftwareForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.UsingSoftware

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UsingSoftwareController @Inject()(usingSoftware: UsingSoftware,
                                        sessionDataService: SessionDataService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends SignUpController with FeatureSwitching {

  private val form: Form[YesNo] = UsingSoftwareForm.usingSoftwareForm


  def view(usingSoftwareForm: Form[YesNo])
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
      } yield {
        usingSoftwareStatus match {
          case Left(_) => throw new InternalServerException("[UsingSoftwareController][show] - Could not fetch software status")
          case Right(maybeYesNo) =>
            Ok(view(
              usingSoftwareForm = form.fill(maybeYesNo)
            ))
        }

      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            usingSoftwareForm = formWithErrors
          ))),
        yesNo =>
          sessionDataService.saveSoftwareStatus(yesNo) map {
            case Left(_) => throw new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer")
            case Right(_) => Redirect(getRedirect(yesNo))
          }
      )
  }

  private def getRedirect(selectedOption: YesNo): Call = {

    selectedOption match {
      case Yes =>
        if (isEnabled(PrePopulate)) {
          controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show()
        } else {
          controllers.individual.routes.WhatYouNeedToDoController.show
        }
      case No => controllers.individual.routes.NoSoftwareController.show
    }

  }

}