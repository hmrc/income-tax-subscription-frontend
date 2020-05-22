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

package services

import config.AppConfig
import connectors.individual.subscription.SubscriptionConnector
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import config.featureswitch.FeatureSwitching
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}
import javax.inject.{Inject, Singleton}
import models.common.AccountingYearModel
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel}
import models.individual.subscription._
import models.{Current, Next, Yes}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(appConfig: AppConfig,
                                    subscriptionConnector: SubscriptionConnector
                                   ) extends FeatureSwitching {


  private[services] def getAccountingPeriod(incomeSourceType: IncomeSourceType,
                                            summaryData: SummaryModel, isAgent: Boolean): Option[AccountingPeriodModel] = {
    if(isAgent) {
      (incomeSourceType, summaryData.matchTaxYear, summaryData.selectedTaxYear) match {
        case (Business, Some(MatchTaxYearModel(Yes)), Some(AccountingYearModel(Next))) => Some(getNextTaxYear)
        case (Business | Both, Some(MatchTaxYearModel(Yes)), _) => Some(getCurrentTaxYear)
        case (Business | Both, _, _) => summaryData.accountingPeriodDate
        case _ => None
      }
    } else {
      (incomeSourceType, summaryData.selectedTaxYear) match {
        case (Business, Some(AccountingYearModel(Next))) => Some(getNextTaxYear)
        case (Business | Both, _) => Some(getCurrentTaxYear)
        case _ => None
      }
    }
  }

  private[services] def buildRequestPost(nino: String, model: SummaryModel, arn: Option[String]): SubscriptionRequest = {
    val businessSection = model.incomeSource.flatMap {
      case Business | Both =>
        for {
          accountingPeriod <- getAccountingPeriod(model.incomeSource.get, model, arn.isDefined)
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
    Logger.debug(s"Submitting subscription with request: $requestPost")
    subscriptionConnector.subscribe(requestPost)
  }

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    Logger.debug(s"Getting subscription for nino=$nino")
    subscriptionConnector.getSubscription(nino)
  }

}
