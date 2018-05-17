/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers.business

import javax.inject.{Inject, Singleton}

import agent.auth.AuthenticatedController
import agent.forms.BusinessNameForm
import agent.models.BusinessNameModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.services.AuthService
import incometax.business.models.AccountingPeriodModel
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.{Both, Business, IncomeSourceType}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

@Singleton
class BusinessNameController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       currentTimeService: CurrentTimeService
                                      ) extends AuthenticatedController {

  def view(businessNameForm: Form[BusinessNameModel],
           isEditMode: Boolean,
           accountingPeriod: Option[AccountingPeriodModel],
           incomeSource: IncomeSourceType
          )(implicit request: Request[_]): Html =
    agent.views.html.business.business_name(
      businessNameForm = businessNameForm,
      postAction = agent.controllers.business.routes.BusinessNameController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode, accountingPeriod, incomeSource)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessName <- keystoreService.fetchBusinessName()
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
        incomeSourceType <- keystoreService.fetchIncomeSource() map (source => IncomeSourceType(source.get.source))
      } yield Ok(view(
        BusinessNameForm.businessNameForm.form.fill(businessName),
        isEditMode = isEditMode,
        accountingPeriod,
        incomeSourceType
      ))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors =>
          for{
            accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
            incomeSourceType <- keystoreService.fetchIncomeSource() map (source => IncomeSourceType(source.get.source))
          } yield BadRequest(view(formWithErrors, isEditMode = isEditMode, accountingPeriod, incomeSourceType)),
        businessName => {
          keystoreService.saveBusinessName(businessName) map (_ =>
            if (isEditMode)
              Redirect(agent.controllers.routes.CheckYourAnswersController.show())
            else
              Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
            )
        }
      )
  }

  def backUrl(isEditMode: Boolean, accountingPeriodModel: Option[AccountingPeriodModel], incomeSource: IncomeSourceType): String = {
    (incomeSource, accountingPeriodModel) match {
      case _ if isEditMode =>
        agent.controllers.routes.CheckYourAnswersController.show().url
      case _ =>
        agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url
    }
  }
}
