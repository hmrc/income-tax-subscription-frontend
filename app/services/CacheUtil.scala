/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.IncomeSourceForm
import models._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    import services.CacheConstants._

    def getIncomeSource()(implicit read: Reads[IncomeSourceModel]): Option[IncomeSourceModel] = cacheMap.getEntry(IncomeSource)

    def getOtherIncome()(implicit read: Reads[OtherIncomeModel]): Option[OtherIncomeModel] = cacheMap.getEntry(OtherIncome)

    def getAccountingPeriodPrior()(implicit read: Reads[AccountingPeriodPriorModel]): Option[AccountingPeriodPriorModel] = cacheMap.getEntry(AccountingPeriodPrior)

    def getAccountingPeriodDate()(implicit read: Reads[AccountingPeriodModel]): Option[AccountingPeriodModel] = cacheMap.getEntry(AccountingPeriodDate)

    def getBusinessName()(implicit read: Reads[BusinessNameModel]): Option[BusinessNameModel] = cacheMap.getEntry(BusinessName)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getTerms()(implicit read: Reads[TermModel]): Option[TermModel] = cacheMap.getEntry(Terms)

    def getSummary()(implicit
                     isrc: Reads[IncomeSourceModel],
                     oirc: Reads[OtherIncomeModel],
                     accP: Reads[AccountingPeriodPriorModel],
                     accD: Reads[AccountingPeriodModel],
                     bus: Reads[BusinessNameModel],
                     accM: Reads[AccountingMethodModel],
                     ter: Reads[TermModel]): SummaryModel = {
      val incomeSource = getIncomeSource()
      incomeSource match {
        case Some(src) =>
          src.source match {
            case IncomeSourceForm.option_property =>
              SummaryModel(
                incomeSource,
                getOtherIncome(),
                terms = getTerms()
              )
            case _ =>
              SummaryModel(
                incomeSource,
                getOtherIncome(),
                getAccountingPeriodPrior(),
                getAccountingPeriodDate(),
                getBusinessName(),
                getAccountingMethod(),
                getTerms()
              )
          }
        case _ => SummaryModel()
      }

    }
  }

}
