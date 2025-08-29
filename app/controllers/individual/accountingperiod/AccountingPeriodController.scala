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

import _root_.config.AppConfig
import _root_.config.featureswitch.FeatureSwitch.EmailCaptureConsent
import _root_.config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.accountingperiod.AccountingPeriodForm.accountingPeriodForm
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.common.BusinessAccountingPeriod
import models.common.BusinessAccountingPeriod._
import play.api.mvc._
import services.{AuditingService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.individual.accountingPeriod.AccountingPeriod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountingPeriodController @Inject()(view: AccountingPeriod,
                                           identify: IdentifierAction,
                                           journeyRefiner: SignUpJourneyRefiner,
                                           subscriptionDetailsService: SubscriptionDetailsService,
                                           auditingService: AuditingService)
                                          (implicit val appConfig: AppConfig,
                                           ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends SignUpBaseController with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchAccountingPeriod(request.reference) flatMap { accountingPeriod =>
      Future.successful(
        Ok(view(
          accountingPeriodForm = accountingPeriodForm.fill(accountingPeriod),
          postAction = routes.AccountingPeriodController.submit,
          backUrl = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url))
      )
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    accountingPeriodForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(
        accountingPeriodForm = formWithErrors,
        postAction = routes.AccountingPeriodController.submit,
        backUrl = controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
      ))),
      accountingPeriod => {
        subscriptionDetailsService.saveAccountingPeriod(request.reference, accountingPeriod) flatMap {
          case Right(_) => accountingPeriod match {
              case SixthAprilToFifthApril =>
                audit(eligible = true, answer = SixthAprilToFifthApril)
                  .map(_ => Redirect(nextPage))
              case FirstAprilToThirtyFirstMarch =>
                audit(eligible = true, answer = FirstAprilToThirtyFirstMarch)
                  .map(_ => Redirect(nextPage))
              case OtherAccountingPeriod =>
                audit(eligible = false, answer = OtherAccountingPeriod)
                  .map(_ => Redirect(routes.AccountingPeriodNonStandardController.show))
            }
          case Left(_) => throw new InternalServerException("[AccountingPeriodController][submit] - Failed to save accounting period")
        }
      }
    )
  }

  private def audit(eligible: Boolean, answer: BusinessAccountingPeriod)
                   (implicit request: Request[AnyContent]): Future[AuditResult] = {
    auditingService.audit(EligibilityAnswerAuditModel(
      eligible = eligible,
      answer = answer match {
        case SixthAprilToFifthApril => "6 april to 5 april"
        case FirstAprilToThirtyFirstMarch => "1 april to 31 march"
        case OtherAccountingPeriod => "other"
      },
      question = "standardAccountingPeriod"
    ))
  }

  private def nextPage: Call = if (isEnabled(EmailCaptureConsent)) {
    controllers.individual.email.routes.CaptureConsentController.show()
  } else {
    controllers.individual.routes.WhatYouNeedToDoController.show
  }

}
