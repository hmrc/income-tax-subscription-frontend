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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{SaveAndRetrieve, ForeignProperty => ForeignPropertyFeature}
import config.featureswitch.FeatureSwitching
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.common.{BusinessIncomeSourceModel, ForeignProperty, SelfEmployed, UkProperty}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import views.html.individual.incometax.incomesource.WhatIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIncomeSourceToSignUpController @Inject()(whatIncomeSourceToSignUp: WhatIncomeSourceToSignUp,
                                                    val auditingService: AuditingService,
                                                    val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        Future.successful(Ok(view(businessIncomeSourceForm)))
      } else {
        Future.failed(new NotFoundException("[WhatIncomeSourceToSignUpController][show] - The save and retrieve feature switch is disabled"))
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      businessIncomeSourceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(form = formWithErrors))),
        businessIncomeSource => {
          val redirect = businessIncomeSource.incomeSource match {
            case SelfEmployed => Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
            case UkProperty => Redirect(controllers.individual.business.routes.PropertyStartDateController.show())
            case ForeignProperty =>
              if(isEnabled(ForeignPropertyFeature)) {
                Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
              } else {
                throw new InternalServerException("[WhatIncomeSourceToSignUpController][submit] - The foreign property feature switch is disabled")
              }
          }

          Future.successful(redirect)
        }
      )
  }

  def backUrl: String = controllers.individual.business.routes.TaskListController.show.url

  private def view(form: Form[BusinessIncomeSourceModel])(implicit request: Request[_]): Html =
    whatIncomeSourceToSignUp(
      incomeSourceForm = form,
      postAction = controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.submit(),
      foreignProperty = isEnabled(ForeignPropertyFeature),
      backUrl = backUrl
    )

  private def businessIncomeSourceForm: Form[BusinessIncomeSourceModel] =
    BusinessIncomeSourceForm.businessIncomeSourceForm(isEnabled(ForeignPropertyFeature))

}
