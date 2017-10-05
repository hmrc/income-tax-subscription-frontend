/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import auth.SignUpController
import config.BaseControllerConfig
import forms.IncomeSourceForm
import models.IncomeSourceModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.{AuthService, KeystoreService}

import scala.concurrent.Future

@Singleton
class IncomeSourceController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService
                                      ) extends SignUpController {

  def showIncomeSource(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        incomeSource => Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm.fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def view(incomeSourceForm: Form[IncomeSourceModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.routes.IncomeSourceController.submitIncomeSource(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def submitIncomeSource(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      IncomeSourceForm.incomeSourceForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(incomeSourceForm = formWithErrors, isEditMode = isEditMode))),
        incomeSource => {
          lazy val linearJourney: Future[Result] =
            keystoreService.saveIncomeSource(incomeSource) flatMap { _ =>
              incomeSource.source match {
                case IncomeSourceForm.option_business => business
                case IncomeSourceForm.option_property => property
                case IncomeSourceForm.option_both => both
              }
            }

          if (!isEditMode)
            linearJourney
          else
            (for {
              oldIncomeSource <- keystoreService.fetchIncomeSource()
            } yield {
              // if what was persisted is the same as the new value then go straight back to summary
              if (oldIncomeSource.fold(false)(i => i.source.equals(incomeSource.source)))
                Future.successful(Redirect(controllers.routes.CheckYourAnswersController.submit()))
              else // otherwise go back to the linear journey
                linearJourney
            }).flatMap(x => x)
        }
      )
  }

  def business(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(controllers.routes.OtherIncomeController.showOtherIncome()))

  def property(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(controllers.routes.OtherIncomeController.showOtherIncome()))

  def both(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(controllers.routes.OtherIncomeController.showOtherIncome()))

  lazy val backUrl: String =
      controllers.routes.CheckYourAnswersController.show().url
}
