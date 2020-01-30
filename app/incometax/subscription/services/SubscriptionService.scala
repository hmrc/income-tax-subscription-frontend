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

package incometax.subscription.services

import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import connectors.individual.subscription.SubscriptionConnector
import core.audit.Logging
import core.config.AppConfig
import core.config.featureswitch.FeatureSwitching
import core.models.{Next, Yes}
import incometax.business.models.{AccountingPeriodModel, AccountingYearModel, MatchTaxYearModel}
import incometax.subscription.models._
import incometax.util.AccountingPeriodUtil._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(applicationConfig: AppConfig,
                                    logging: Logging,
                                    subscriptionConnector: SubscriptionConnector
                                   ) extends FeatureSwitching {


  private[services] def getAccountingPeriod(incomeSourceType: IncomeSourceType,
                                            summaryData: SummaryModel): Option[AccountingPeriodModel] = {
    (incomeSourceType, summaryData.matchTaxYear, summaryData.selectedTaxYear) match {
      case (Business, Some(MatchTaxYearModel(Yes)), Some(AccountingYearModel(Next))) => Some(getNextTaxYear)
      case (Business | Both, Some(MatchTaxYearModel(Yes)), _) => Some(getCurrentTaxYear)
      case (Business | Both, _, _) => summaryData.accountingPeriodDate
      case _ => None
    }
  }

  private[services] def buildRequestPost(nino: String, model: SummaryModel, arn: Option[String]): SubscriptionRequest = {
    val businessSection = model.incomeSource.flatMap {
      case Business | Both =>
        for {
          accountingPeriod <- getAccountingPeriod(model.incomeSource.get, model)
          accountingMethod <- model.accountingMethod map (_.accountingMethod)
          businessName = model.businessName map (_.businessName)
        } yield BusinessIncomeModel(businessName, accountingPeriod, accountingMethod)
      case _ => None
    }

    val propertySection = model.incomeSource flatMap {
      case Property | Both =>
        Some(PropertyIncomeModel(model.accountingMethodProperty.map(_.propertyAccountingMethod)))
      case _ => None
    }


    SubscriptionRequest(nino, arn, businessSection, propertySection)
  }

  def submitSubscription(nino: String,
                         summaryData: SummaryModel,
                         arn: Option[String]
                        )(implicit hc: HeaderCarrier): Future[SubscriptionResponse] = {

      val requestPost = buildRequestPost(nino, summaryData, arn)
      logging.debug(s"Submitting subscription with request: $requestPost")
      subscriptionConnector.subscribe(requestPost)
  }

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    logging.debug(s"Getting subscription for nino=$nino")
    subscriptionConnector.getSubscription(nino)
  }

}
