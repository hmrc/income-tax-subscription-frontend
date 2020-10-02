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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import forms.agent.BusinessNameForm
import forms.agent.BusinessNameForm.businessName
import javax.inject.{Inject, Singleton}
import models.common.{BusinessNameModel, IncomeSourceModel}
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessNameController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                       appConfig: AppConfig) extends AuthenticatedController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean, backUrl: String)(implicit request: Request[_]): Html = {
    views.html.agent.business.business_name(
      businessNameForm = businessNameForm,
      postAction = controllers.agent.business.routes.BusinessNameController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessName <- subscriptionDetailsService.fetchBusinessName()
        incomeSource <- subscriptionDetailsService.fetchIncomeSource()
      } yield Ok(view(
        BusinessNameForm.businessNameForm.form.fill(businessName),
        isEditMode = isEditMode,
        backUrl = backUrl(isEditMode, incomeSource.getOrElse(
          throw new InternalServerException("User is missing income source details in Subscription Details")))
      ))
  }

  private def redirectLocation( currentAnswer: BusinessNameModel, isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      incomeSources <- subscriptionDetailsService.fetchIncomeSource
    } yield {
      incomeSources match {
        case None =>
          Redirect(controllers.agent.routes.IncomeSourceController.show())
        case _ if isEditMode =>
          Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        case Some(IncomeSourceModel(false, false, false)) =>
          Logger.error("[BusinessNameController][Redirect Location] The User has attempted to submit a Business name with no valid sources of income")
          Redirect(controllers.agent.routes.IncomeSourceController.show())
        case _ =>
          Redirect(controllers.agent.business.routes.BusinessAccountingMethodController.show(isEditMode))
      }
    }
  }
  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors,
          isEditMode = isEditMode, backUrl = controllers.agent.business.routes.WhatYearToSignUpController.show().url))),
        businessName => for {
          redirect <- redirectLocation(businessName, isEditMode)
          _ <- subscriptionDetailsService.saveBusinessName(businessName)
        } yield redirect
      )
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel): String = {
    if (isEditMode) controllers.agent.routes.CheckYourAnswersController.show().url
    else
      incomeSourceModel match {
        case IncomeSourceModel(true, false, false) => controllers.agent.business.routes.WhatYearToSignUpController.show().url
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
  }
}
