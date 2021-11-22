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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import models.common.AccountingYearModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import views.html.individual.incometax.business.TaxYearCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearCheckYourAnswersController @Inject()(val checkYourAnswersView: TaxYearCheckYourAnswers,
                                                  val accountingPeriodService: AccountingPeriodService,
                                                  val auditingService: AuditingService,
                                                  val authService: AuthService,
                                                  val subscriptionDetailsService: SubscriptionDetailsService)
                                                 (implicit val ec: ExecutionContext,
                                                  val appConfig: AppConfig,
                                                  mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          subscriptionDetailsService.fetchSelectedTaxYear(reference) map { maybeAccountingYearModel =>
            Ok(checkYourAnswersView(
              postAction = controllers.individual.business.routes.TaxYearCheckYourAnswersController.submit(),
              viewModel = maybeAccountingYearModel,
              accountingPeriodService = accountingPeriodService,
              backUrl = backUrl(isEditMode)
            ))
          }
        } else {
          Future.failed(new NotFoundException("[CheckYourAnswersController][submit] - The save and retrieve feature switch is disabled"))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) flatMap { maybeAccountingYearModel =>
          val accountingYearModel: AccountingYearModel = maybeAccountingYearModel.getOrElse(
            throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not retrieve accounting year")
          )
          subscriptionDetailsService.saveSelectedTaxYear(reference, accountingYearModel.copy(confirmed = true)) map { _ =>
            Redirect(routes.TaskListController.show())
          }
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = if (isEditMode) {
    controllers.individual.business.routes.TaskListController.show().url
  } else {
    controllers.individual.business.routes.WhatYearToSignUpController.show().url
  }
}
