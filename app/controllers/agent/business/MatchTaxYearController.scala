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

import agent.auth.AuthenticatedController
import core.config.AppConfig
import forms.agent.MatchTaxYearForm
import javax.inject.Inject
import models.No
import models.individual.business.MatchTaxYearModel
import models.individual.subscription.Both
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.AuthService
import services.agent.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

class MatchTaxYearController @Inject()(val authService: AuthService,
                                       val messagesApi: MessagesApi,
                                       keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends AuthenticatedController {

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
      keystoreService.fetchMatchTaxYear() map { matchTaxYear =>
        Ok(view(MatchTaxYearForm.matchTaxYearForm.fill(matchTaxYear), isEditMode))
      }
  }

  private def redirectLocation(currentAnswer: MatchTaxYearModel, isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      matchTaxYear <- keystoreService.fetchMatchTaxYear
      incomeSources <- keystoreService.fetchIncomeSource
    } yield {
      (currentAnswer, incomeSources) match {
        case (_, None) =>
          Redirect(controllers.agent.routes.IncomeSourceController.show())
        case (_, _) if isEditMode && matchTaxYear.contains(currentAnswer) =>
          Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        case (MatchTaxYearModel(No), _) =>
          Redirect(controllers.agent.business.routes.BusinessAccountingPeriodDateController.show(isEditMode))
        case (_, Some(Both)) =>
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
          _ <- keystoreService.saveMatchTaxYear(matchTaxYear)
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
