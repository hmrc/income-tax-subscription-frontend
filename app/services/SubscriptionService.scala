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

import config.featureswitch.FeatureSwitching
import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import connectors.individual.subscription.{MultipleIncomeSourcesSubscriptionConnector, SubscriptionConnector}
import javax.inject.{Inject, Singleton}
import models.common.AccountingYearModel
import models.individual.business.{AccountingPeriodModel, BusinessSubscriptionDetailsModel, MatchTaxYearModel}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
import models.{IndividualSummary, Next, SummaryModel, Yes}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil.{getCurrentTaxYear, getNextTaxYear}

import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(multipleIncomeSourcesSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector,
                                    subscriptionConnector: SubscriptionConnector
                                   ) extends FeatureSwitching {


  private[services] def getAccountingPeriod(summaryData: SummaryModel, isAgent: Boolean): Option[AccountingPeriodModel] = {
    if (isAgent) {
      (summaryData.incomeSource.get, summaryData.matchTaxYear, summaryData.selectedTaxYear) match {
        case (Business, Some(MatchTaxYearModel(Yes)), Some(AccountingYearModel(Next))) => Some(getNextTaxYear)
        case (Business | Both, Some(MatchTaxYearModel(Yes)), _) => Some(getCurrentTaxYear)
        case (Business | Both, _, _) => summaryData.accountingPeriodDate
        case _ => None
      }
    } else {
      (summaryData.incomeSourceIndiv.get, summaryData.selectedTaxYear) match {
        case (IncomeSourceModel(true, false, _), Some(AccountingYearModel(Next))) => Some(getNextTaxYear)
        case (IncomeSourceModel(true, _, _), _) => Some(getCurrentTaxYear)
        case _ => None
      }
    }
  }

  private[services] def buildRequestPost(nino: String, model: SummaryModel, arn: Option[String]): SubscriptionRequest = {
    if (arn.isDefined) {
      val businessSection = model.incomeSource.flatMap {
        case Business | Both =>
          for {
            accountingPeriod <- getAccountingPeriod(model, arn.isDefined)
            accountingMethod <- model.accountingMethod map (_.accountingMethod)
            businessName = model.businessName map (_.businessName)
          } yield BusinessIncomeModel(businessName, accountingPeriod, accountingMethod)
        case _ => None
      }

      val propertySection = model.incomeSource flatMap {
        case UkProperty | Both =>
          Some(PropertyIncomeModel(model.accountingMethodProperty.map(_.propertyAccountingMethod)))
        case _ => None
      }


      SubscriptionRequest(nino, arn, businessSection, propertySection)
    }
    else {

      val businessSection = model.incomeSourceIndiv.flatMap {
        case IncomeSourceModel(true, _, _) =>
          for {
            accountingPeriod <- getAccountingPeriod(model, arn.isDefined)
            accountingMethod <- model.accountingMethod map (_.accountingMethod)
            businessName = model.businessName map (_.businessName)
          } yield BusinessIncomeModel(businessName, accountingPeriod, accountingMethod)
        case _ => None
      }

      val propertySection = model.incomeSourceIndiv flatMap {
        case IncomeSourceModel(_, true, _) =>
          Some(PropertyIncomeModel(model.accountingMethodProperty.map(_.propertyAccountingMethod)))
        case _ => None
      }


      SubscriptionRequest(nino, arn, businessSection, propertySection)
    }

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

  def signUpIncomeSources(nino: String)(implicit hc: HeaderCarrier): Future[PostSignUpIncomeSourcesResponse] = {
    Logger.debug(s"SignUp IncomeSources request for nino:$nino")
    multipleIncomeSourcesSubscriptionConnector.signUp(nino)
  }

  def createIncomeSources(mtdbsa: String, individualSummary: IndividualSummary)
                         (implicit hc: HeaderCarrier): Future[PostCreateIncomeSourceResponse] = {
    Logger.debug(s"Create IncomeSources request for MTDSA Id:$mtdbsa")
    val businessSubscriptionDetailsModel: BusinessSubscriptionDetailsModel =
      individualSummary.copy(
        accountingPeriodDate = getAccountingPeriod(individualSummary, false)).toBusinessSubscriptionDetailsModel

    multipleIncomeSourcesSubscriptionConnector.createIncomeSources(mtdbsa, businessSubscriptionDetailsModel)
  }
}
