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

package controllers.individual.business

import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import forms.individual.business.BusinessStartDateForm
import forms.individual.business.BusinessStartDateForm.businessStartDateForm
import javax.inject.{Inject, Singleton}
import models.individual.business.{BusinessStartDate, SelfEmploymentData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, MultipleSelfEmploymentsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.individual.ImplicitDateFormatter
import views.html.individual.incometax.business.business_start_date

import scala.concurrent.ExecutionContext


@Singleton
class BusinessStartDateController @Inject()(mcc: MessagesControllerComponents,
                                            multipleSelfEmploymentsService: MultipleSelfEmploymentsService,
                                            authService: AuthService,
                                            val languageUtils: LanguageUtils)
                                           (implicit val ec: ExecutionContext, val appConfig: AppConfig) extends FrontendController(mcc)
  with I18nSupport with ImplicitDateFormatter with FeatureSwitching {

  def view(businessStartDateForm: Form[BusinessStartDate], id: String, businesses: Seq[SelfEmploymentData], isEditMode: Boolean)
          (implicit request: Request[AnyContent]): Html = {
    if(isEnabled(ReleaseFour)) {
      business_start_date(
        businessStartDateForm = businessStartDateForm,
        postAction = controllers.individual.business.routes.BusinessStartDateController.submit(id, isEditMode),
        isEditMode,
        backUrl = backUrl(id, businesses, isEditMode)
      )
    } else {
      throw new InternalServerException("Page is disabled")
    }
  }


  def show(id: String, isEditMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      multipleSelfEmploymentsService.fetchAllBusinesses.flatMap {
        case businesses =>
          multipleSelfEmploymentsService.fetchBusinessStartDate(id).map {
            case Some(businessStartDateData) => Ok(view(form.fill(businessStartDateData), id, businesses, isEditMode))
            case _ => Ok(view(form, id, businesses, isEditMode))
          }
      }.recoverWith {
        case ex:Exception => throw new InternalServerException(ex.getMessage)
      }
    }
  }


  def submit(id: String, isEditMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      form.bindFromRequest.fold(
        formWithErrors => {
          multipleSelfEmploymentsService.fetchAllBusinesses.map {
            case businesses => BadRequest(view(formWithErrors, id, businesses, isEditMode))
          }.recoverWith {
            case ex: Exception => throw new InternalServerException(ex.getMessage)
          }
        },
        businessStartDateData =>
          multipleSelfEmploymentsService.saveBusinessStartDate(id, businessStartDateData).map(_ =>
            if (isEditMode) {
              Redirect(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show())
            } else {
              Redirect(controllers.individual.business.routes.BusinessNameController.show(id))
            }
          )
      )
    }
  }

  def backUrl(id: String, businesses: Seq[SelfEmploymentData], isEditMode: Boolean): String = {
    val isFirstCompleteBusiness: Boolean = businesses.find(_.isComplete).exists(_.id == id)
    if (isEditMode || !isFirstCompleteBusiness) {
      controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url
    } else {
      controllers.individual.business.routes.WhatYearToSignUpController.show().url
    }
  }

  def form(implicit request: Request[_]): Form[BusinessStartDate] = {
    businessStartDateForm(BusinessStartDateForm.minStartDate.toLongDate)
  }

}
