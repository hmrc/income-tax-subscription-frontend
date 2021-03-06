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

package controllers.individual.subscription

import auth.individual.PostSubmissionController
import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AccountingPeriodService, AuditingService, AuthService, SubscriptionDetailsService}
import utilities.SubscriptionDataUtil._
import views.html.individual.incometax.subscription.SignUpComplete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val auditingService: AuditingService,
                                       val authService: AuthService,
                                       accountingPeriodService: AccountingPeriodService,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       signUpComplete:SignUpComplete
                                      )
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController with FeatureSwitching {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      val endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear
      val updatesAfter = accountingPeriodService.updateDatesAfter()
      val updatesBefore = accountingPeriodService.updateDatesBefore()

      subscriptionDetailsService.fetchAll() map { cacheMap =>
        Ok(signUpComplete(cacheMap.getSummary(isReleaseFourEnabled = isEnabled(ReleaseFour)), endYearOfCurrentTaxPeriod, updatesBefore, updatesAfter))
      }
  }

}
