/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual.accountingperiod
import config.AppConfig
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.accountingperiod.AccountingPeriodNonStandardForm.nonStandardAccountingPeriodForm
import models.{No, Yes}
import models.common.AccountingYearModel
import models.Next
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.accountingPeriod.AccountingPeriodNonStandard
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class AccountingPeriodNonStandardController @Inject()(view: AccountingPeriodNonStandard,
                                                      identify: IdentifierAction,
                                                      journeyRefiner: SignUpJourneyRefiner,
                                                      subscriptionDetailsService: SubscriptionDetailsService)
                                                     (implicit val mcc: MessagesControllerComponents,
                                                      ec: ExecutionContext,
                                                      val appConfig: AppConfig) extends SignUpBaseController {
  val show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      nonStandardAccountingPeriodForm = nonStandardAccountingPeriodForm,
      postAction = routes.AccountingPeriodNonStandardController.submit,
      backUrl = controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
    ))
  }
  val submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    nonStandardAccountingPeriodForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          nonStandardAccountingPeriodForm = formWithErrors,
          postAction = routes.AccountingPeriodNonStandardController.submit,
          backUrl = controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
        ))),
      {
        case Yes =>
          subscriptionDetailsService.saveSelectedTaxYear(request.reference, AccountingYearModel(Next)).map {
            case Right(_) =>
              Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
            case Left(_) =>
              throw new InternalServerException(
                "[AccountingPeriodNonStandardController][submit] Could not save answer for selected next tax year"
              )
          }
        case No => Future.successful(Redirect(routes.AccountingPeriodNotSupportedController.show))
      }
    )
  }
}