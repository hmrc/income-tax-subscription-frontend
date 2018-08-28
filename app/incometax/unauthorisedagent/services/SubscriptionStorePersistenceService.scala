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

package incometax.unauthorisedagent.services

import javax.inject.{Inject, Singleton}

import agent.models.AccountingPeriodPriorModel
import agent.services.CacheUtil._
import agent.services.KeystoreService
import core.config.AppConfig
import core.models.{No, Yes}
import incometax.subscription.models.IncomeSourceType
import incometax.unauthorisedagent.connectors.SubscriptionStoreConnector
import incometax.unauthorisedagent.httpparsers.StoreSubscriptionResponseHttpParser.StoreSubscriptionResponse
import incometax.unauthorisedagent.models.StoredSubscription
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionStorePersistenceService @Inject()(subscriptionStoreConnector: SubscriptionStoreConnector,
                                                    keystoreService: KeystoreService,
                                                    appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def storeSubscription(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[StoreSubscriptionResponse] =
    getSubscriptionData(arn).flatMap(subscriptionData => subscriptionStoreConnector.storeSubscriptionData(nino, subscriptionData))

  private[services] def getSubscriptionData(arn: String)(implicit hc: HeaderCarrier): Future[StoredSubscription] =
    for {
      optCache <- keystoreService.fetchAll()
      cache = optCache.get
      incomeSource = cache.getIncomeSource
      otherIncome = cache.getOtherIncome()
      accountingPeriodPrior = cache.getAccountingPeriodPrior()
      accountingPeriodDates = cache.getAccountingPeriodDate()
      businessName = cache.getBusinessName()
      accountingMethod = cache.getAccountingMethod()
    } yield StoredSubscription(
      arn,
      IncomeSourceType(incomeSource.get.source),
      otherIncome.map {
        case Yes => true
        case No => false
      }.get,
      accountingPeriodPrior.map {
        case AccountingPeriodPriorModel(Yes) => true
        case AccountingPeriodPriorModel(No) => false
      },
      accountingPeriodDates.map(_.startDate),
      accountingPeriodDates.map(_.endDate),
      businessName.map(_.businessName),
      accountingMethod.map(_.accountingMethod)
    )


}
