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

package incometax.subscription.services

import javax.inject.{Inject, Singleton}

import core.audit.Logging
import core.config.AppConfig
import core.models.{No, Yes}
import incometax.business.models.{AccountingPeriodModel, MatchTaxYearModel}
import incometax.subscription.connectors.SubscriptionConnector
import incometax.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import incometax.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import incometax.subscription.models._
import incometax.util.AccountingPeriodUtil._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(applicationConfig: AppConfig,
                                    logging: Logging,
                                    subscriptionConnector: SubscriptionConnector) {


  private[services] def getAccountingPeriod(incomeSourceType: IncomeSourceType,
                                            summaryData: SummaryModel,
                                            arn: Option[String]): Option[AccountingPeriodModel] =
    if (arn.isEmpty) {
      (incomeSourceType, summaryData.matchTaxYear) match {
        case (Business | Both, Some(MatchTaxYearModel(Yes))) => Some(getCurrentTaxYear)
        case (Business | Both, Some(MatchTaxYearModel(No))) => summaryData.accountingPeriod
        case _ => None
      }
    } else {
      incomeSourceType match {
        case Property => None
        case _ => summaryData.accountingPeriod
      }
    }

  private[services] def buildRequest(nino: String, summaryData: SummaryModel, arn: Option[String]): SubscriptionRequest = {
    val incomeSource = summaryData.incomeSource.get
    val accountingPeriod = getAccountingPeriod(incomeSource, summaryData, arn)
    val (accountingPeriodStart, accountingPeriodEnd) = (accountingPeriod.map(_.startDate), accountingPeriod.map(_.endDate))
    val cashOrAccruals = summaryData.accountingMethod map (_.accountingMethod)
    val tradingName = summaryData.businessName map (_.businessName)

    SubscriptionRequest(
      nino = nino,
      arn = arn,
      incomeSource = incomeSource,
      accountingPeriodStart = accountingPeriodStart,
      accountingPeriodEnd = accountingPeriodEnd,
      cashOrAccruals = cashOrAccruals,
      tradingName = tradingName
    )
  }

  def submitSubscription(nino: String,
                         summaryData: SummaryModel,
                         arn: Option[String]
                        )(implicit hc: HeaderCarrier): Future[SubscriptionResponse] = {
    val request = buildRequest(nino, summaryData, arn)
    logging.debug(s"Submitting subscription with request: $request")
    subscriptionConnector.subscribe(request)
  }

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    logging.debug(s"Getting subscription for nino=$nino")
    subscriptionConnector.getSubscription(nino)
  }

}
