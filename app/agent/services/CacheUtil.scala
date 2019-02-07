/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.services


import agent.models.AccountingPeriodPriorModel
import core.models.YesNo
import incometax.business.models.{AccountingMethodModel, AccountingPeriodModel, BusinessNameModel}
import incometax.incomesource.models.OtherIncomeModel
import incometax.subscription.models.{AgentSummary, IncomeSourceType, Property}
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    import CacheConstants._

    def getIncomeSource()(implicit read: Reads[IncomeSourceType]): Option[IncomeSourceType] = cacheMap.getEntry(IncomeSource)

    def getOtherIncome()(implicit read: Reads[YesNo]): Option[YesNo] = cacheMap.getEntry(OtherIncome)

    def getAccountingPeriodPrior()(implicit read: Reads[AccountingPeriodPriorModel]): Option[AccountingPeriodPriorModel] = cacheMap.getEntry(AccountingPeriodPrior)

    def getAccountingPeriodDate()(implicit read: Reads[AccountingPeriodModel]): Option[AccountingPeriodModel] = cacheMap.getEntry(AccountingPeriodDate)

    def getBusinessName()(implicit read: Reads[BusinessNameModel]): Option[BusinessNameModel] = cacheMap.getEntry(BusinessName)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getTerms()(implicit read: Reads[Boolean]): Option[Boolean] = cacheMap.getEntry(Terms)

    def getSummary()(implicit
                     isrc: Reads[IncomeSourceType],
                     oirc: Reads[YesNo],
                     accP: Reads[AccountingPeriodPriorModel],
                     accD: Reads[AccountingPeriodModel],
                     bus: Reads[BusinessNameModel],
                     accM: Reads[AccountingMethodModel],
                     ter: Reads[Boolean]): AgentSummary = {
      val incomeSource = getIncomeSource()
      incomeSource match {
        case Some(src) =>
          src match {
            case Property =>
              AgentSummary(
                incomeSource,
                otherIncome = getOtherIncome(),
                terms = getTerms()
              )
            case _ =>
              AgentSummary(
                incomeSource = incomeSource,
                otherIncome = getOtherIncome(),
                accountingPeriodPrior = getAccountingPeriodPrior(),
                accountingPeriod = getAccountingPeriodDate(),
                businessName = getBusinessName(),
                businessPhoneNumber = None,
                businessAddress = None,
                businessStartDate = None,
                accountingMethod = getAccountingMethod(),
                terms = getTerms()
              )
          }
        case _ => AgentSummary()
      }

    }
  }

}
