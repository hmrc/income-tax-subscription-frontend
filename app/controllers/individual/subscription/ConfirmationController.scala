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

package controllers.individual.subscription

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import auth.individual.PostSubmissionController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{PropertyNextTaxYear, ReleaseFour}
import config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import models.Next
import models.common.AccountingYearModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataUtil._
import utilities.{AccountingPeriodUtil, ITSASessionKeys}
import views.html.individual.incometax.subscription.sign_up_complete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController with FeatureSwitching {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      val startTime = LocalDateTime.parse(request.session.get(ITSASessionKeys.StartTime).get)
      val endTime = java.time.LocalDateTime.now()
      val journeyDuration = ChronoUnit.MILLIS.between(startTime, endTime).toInt
      val declarationCurrentYear = (AccountingPeriodUtil.getCurrentTaxYear.taxEndYear + 1).toString
      val declarationNextYear = (AccountingPeriodUtil.getNextTaxYear.taxEndYear + 1).toString


      subscriptionDetailsService.fetchAll() map { cacheMap =>
        cacheMap.getSummary(isReleaseFourEnabled = isEnabled(ReleaseFour), isPropertyNextTaxYearEnabled = isEnabled(PropertyNextTaxYear))
      } map { summary =>
        summary.incomeSource match {
          case Some(_) => {
            summary.selectedTaxYear match {
              case Some(AccountingYearModel(Next)) =>
                Ok(sign_up_complete(journeyDuration, summary, declarationNextYear))
              case _ =>
                Ok(sign_up_complete(journeyDuration, summary, declarationCurrentYear))
            }
          }
          case _ =>
            throw new InternalServerException("Confirmation Controller, call to show confirmation with invalid income source")
        }
      }
  }

}
