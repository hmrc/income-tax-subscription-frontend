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
import forms.agent.MatchTaxYearForm
import javax.inject.{Inject, Singleton}
import models.No
import models.common.IncomeSourceModel
import models.individual.business.MatchTaxYearModel
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchTaxYearController @Inject()(val authService: AuthService,

                                       subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                       appConfig: AppConfig) extends AuthenticatedController {

  private def view(matchTaxYearForm: Form[MatchTaxYearModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html = {
    views.html.agent.business.match_to_tax_year(
      matchTaxYearForm,
      controllers.agent.business.routes.MatchTaxYearController.submit(isEditMode),
      backUrl(isEditMode),
      isEditMode
    )
  }


  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchMatchTaxYear() map { matchTaxYear =>
        Ok(view(MatchTaxYearForm.matchTaxYearForm.fill(matchTaxYear), isEditMode))
      }
  }

  private def redirectLocation(currentAnswer: MatchTaxYearModel, isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      matchTaxYear <- subscriptionDetailsService.fetchMatchTaxYear
      incomeSources <- subscriptionDetailsService.fetchIncomeSource
    } yield {
      (currentAnswer, incomeSources) match {
        case (_, None) =>
          Redirect(controllers.agent.routes.IncomeSourceController.show())
        case (_, _) if isEditMode && matchTaxYear.contains(currentAnswer) =>
          Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        case (MatchTaxYearModel(No), _) =>
          Redirect(controllers.agent.business.routes.BusinessAccountingPeriodDateController.show(isEditMode))
        case (_, Some(IncomeSourceModel(true, true, _))) =>
          Redirect(controllers.agent.business.routes.BusinessAccountingMethodController.show(isEditMode))
        case _ =>
          Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show(isEditMode))
      }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      MatchTaxYearForm.matchTaxYearForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(matchTaxYearForm = formWithErrors, isEditMode = isEditMode))),
        matchTaxYear => for {
          redirect <- redirectLocation(matchTaxYear, isEditMode)
          _ <- subscriptionDetailsService.saveMatchTaxYear(matchTaxYear)
        } yield redirect
      )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.business.routes.BusinessNameController.show().url
    }
  }
}
