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

package incometax.incomesource.controllers

import javax.inject.{Inject, Singleton}

import cats.implicits._
import core.auth.AuthPredicate.AuthPredicate
import core.auth.{IncomeTaxSAUser, SignUpController}
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.IncomeSourceType
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class IncomeSourceController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       val currentTimeService: CurrentTimeService
                                      ) extends SignUpController {

  override def defaultSignUpPredicates: AuthPredicate[IncomeTaxSAUser] = subscriptionPredicates |+| oldIncomeSourceFlowFeature

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        incomeSource => Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm.fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def view(incomeSourceForm: Form[IncomeSourceType], isEditMode: Boolean)(implicit request: Request[_]): Html =
    incometax.incomesource.views.html.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = incometax.incomesource.controllers.routes.IncomeSourceController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
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
                Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.submit()))
              else // otherwise go back to the linear journey
                linearJourney
            }).flatMap(x => x)
        }
      )
  }

  def business(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show()))

  def property(implicit request: Request[_]): Future[Result] = {
    if (applicationConfig.taxYearDeferralEnabled && currentTimeService.getTaxYearEndForCurrentDate <= 2018)
      Future.successful(Redirect(incometax.incomesource.controllers.routes.CannotReportYetController.show()))
    else
      Future.successful(Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show()))
  }

  def both(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show()))

  lazy val backUrl: String =
    incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
}
