/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.tasklist.taxyear

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.{ReferenceRetrieval, TaxYearNavigationHelper}
import models.common.AccountingYearModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.taxyear.TaxYearCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaxYearCheckYourAnswersController @Inject()(checkYourAnswersView: TaxYearCheckYourAnswers,
                                                  referenceRetrieval: ReferenceRetrieval,
                                                  accountingPeriodService: AccountingPeriodService,
                                                  subscriptionDetailsService: SubscriptionDetailsService)
                                                 (val auditingService: AuditingService,
                                                  val appConfig: AppConfig,
                                                  val authService: AuthService,
                                                  val getEligibilityStatusService: GetEligibilityStatusService,
                                                  val mandationStatusService: MandationStatusService)
                                                 (implicit val ec: ExecutionContext,
                                                  mcc: MessagesControllerComponents)
  extends SignUpController with TaxYearNavigationHelper {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      handleUnableToSelectTaxYearIndividual {
        referenceRetrieval.getIndividualReference flatMap { reference =>
          subscriptionDetailsService.fetchSelectedTaxYear(reference) map { maybeAccountingYearModel =>
            Ok(checkYourAnswersView(
              postAction = controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit(),
              viewModel = maybeAccountingYearModel,
              accountingPeriodService = accountingPeriodService,
              backUrl = backUrl(isEditMode)
            ))
          }
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) flatMap { maybeAccountingYearModel =>
          val accountingYearModel: AccountingYearModel = maybeAccountingYearModel.getOrElse(
            throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not retrieve accounting year")
          )
          subscriptionDetailsService.saveSelectedTaxYear(reference, accountingYearModel.copy(confirmed = true)) map {
            case Right(_) => Redirect(controllers.individual.tasklist.routes.TaskListController.show())
            case Left(_) => throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not confirm accounting year")
          }
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = if (isEditMode) {
    controllers.individual.tasklist.routes.TaskListController.show().url
  } else {
    controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
  }

}
