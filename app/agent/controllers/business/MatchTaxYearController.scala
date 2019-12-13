/*
 * Copyright 2019 HM Revenue & Customs
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

import agent.auth.AuthenticatedController
import agent.forms.MatchTaxYearForm
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.FeatureSwitching
import core.models.{No, Yes}
import core.services.AuthService
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models.Both
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}

class MatchTaxYearController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService)
                                      (implicit val ec: ExecutionContext) extends AuthenticatedController with FeatureSwitching {

  private def view(matchTaxYearForm: Form[MatchTaxYearModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Html] =
    backUrl(isEditMode).map { backUrl =>
      agent.views.html.business.match_to_tax_year(
        matchTaxYearForm,
        agent.controllers.business.routes.MatchTaxYearController.submit(isEditMode),
        backUrl,
        isEditMode
      )
    }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchMatchTaxYear() flatMap { matchTaxYear =>
        view(MatchTaxYearForm.matchTaxYearForm.fill(matchTaxYear), isEditMode).map(html => Ok(html))
      }
  }

  private def redirectLocation(currentAnswer: MatchTaxYearModel, isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      matchTaxYear <- keystoreService.fetchMatchTaxYear
      incomeSources <- keystoreService.fetchIncomeSource
    } yield {
      (currentAnswer, incomeSources) match {
        case (_, None) =>
          Redirect(agent.controllers.routes.IncomeSourceController.show())
        case (_, _) if isEditMode && matchTaxYear.contains(currentAnswer) =>
          Redirect(agent.controllers.routes.CheckYourAnswersController.show())
        case (MatchTaxYearModel(No), _) =>
          Redirect(agent.controllers.business.routes.BusinessAccountingPeriodDateController.show(isEditMode))
        case (_, Some(Both)) =>
          Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show(isEditMode))
        case _ =>
          Redirect(agent.controllers.business.routes.WhatYearToSignUpController.show(isEditMode))
      }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      MatchTaxYearForm.matchTaxYearForm.bindFromRequest.fold(
        formWithErrors => view(matchTaxYearForm = formWithErrors, isEditMode = isEditMode).map(html => BadRequest(html)),
        matchTaxYear => for {
          redirect <- redirectLocation(matchTaxYear, isEditMode)
          _ <- keystoreService.saveMatchTaxYear(matchTaxYear)
        } yield redirect
      )
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[_]): Future[String] = {
    if (isEditMode) {
      Future.successful(agent.controllers.routes.CheckYourAnswersController.show().url)
    } else {
      keystoreService.fetchAccountingPeriodPrior() map {
        case Some(currentPeriodPrior) => currentPeriodPrior.currentPeriodIsPrior match {
          case Yes =>
            agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
          case No =>
            agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
        }
        case _ => throw new InternalServerException(s"Internal Server Error - No Accounting Period Prior answer retrieved from keystore")
      }
    }
  }
}