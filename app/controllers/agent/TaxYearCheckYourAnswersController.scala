/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import models.common.AccountingYearModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import views.html.agent.business.TaxYearCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearCheckYourAnswersController @Inject()(val checkYourAnswersView: TaxYearCheckYourAnswers,
                                                  val auditingService: AuditingService,
                                                  val authService: AuthService,
                                                  val subscriptionDetailsService: SubscriptionDetailsService)
                                                 (implicit val ec: ExecutionContext,
                                                  val appConfig: AppConfig,
                                                  mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          subscriptionDetailsService.fetchSelectedTaxYear(reference) map { maybeAccountingYearModel =>
            Ok(checkYourAnswersView(
              postAction = controllers.agent.routes.TaxYearCheckYourAnswersController.submit(),
              viewModel = maybeAccountingYearModel,
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
      withAgentReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) flatMap { maybeAccountingYearModel =>
          val accountingYearModel: AccountingYearModel = maybeAccountingYearModel.getOrElse(
            throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not retrieve accounting year")
          )
          subscriptionDetailsService.saveSelectedTaxYear(reference, accountingYearModel.copy(confirmed = true)) map { _ =>
            //need to change redirect route to agent task list when it has been built
            Redirect(controllers.agent.routes.IncomeSourceController.show().url)
          }
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = if (isEditMode) {
    //need to change redirect route to agent task list when it has been built
    controllers.agent.routes.IncomeSourceController.show().url
  } else {
    controllers.agent.routes.WhatYearToSignUpController.show(isEditMode).url
  }
}
