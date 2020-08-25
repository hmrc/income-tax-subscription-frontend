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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import forms.individual.incomesource.IncomeSourceForm
import javax.inject.{Inject, Singleton}
import models.individual.incomesource._
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(incomeSourceForm: Form[IncomeSourceModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.individual.incometax.incomesource.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.individual.incomesource.routes.IncomeSourceController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl,
      foreignProperty = isEnabled(ForeignProperty)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchIndividualIncomeSource() map { incomeSource =>
        Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm
          .fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      IncomeSourceForm.incomeSourceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(incomeSourceForm = formWithErrors, isEditMode = isEditMode))),
        incomeSource => {
          lazy val linearJourney: Future[Result] =
            subscriptionDetailsService.saveIndividualIncomeSource(incomeSource) map { _ =>
              incomeSource match {
                case IncomeSourceModel(true, _, _) =>
                  Redirect(controllers.individual.business.routes.BusinessNameController.show())
                case IncomeSourceModel(_, true, _) =>
                  if (isEnabled(ReleaseFour)) Redirect(controllers.individual.business.routes.PropertyCommencementDateController.show())
                  else Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
                case IncomeSourceModel(_, _, true) =>
                  if (isEnabled(ForeignProperty)) Redirect(controllers.individual.business.routes.OverseasPropertyCommencementDateController.show())
                  else Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
                case _ =>
                  Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
              }
            }

          if (!isEditMode) {
            linearJourney
          } else {
            subscriptionDetailsService.fetchIndividualIncomeSource() flatMap {
              case Some(`incomeSource`) => Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.submit()))
              case _ => linearJourney
            }
          }
        }
      )
  }

  lazy val backUrl: String =
    controllers.individual.subscription.routes.CheckYourAnswersController.show().url
}