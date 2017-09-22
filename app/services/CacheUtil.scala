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
import models.address.Address
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

    def getBusinessPhoneNumber()(implicit read: Reads[BusinessPhoneNumberModel]): Option[BusinessPhoneNumberModel] = cacheMap.getEntry(BusinessPhoneNumber)

    def getBusinessAddress()(implicit read: Reads[Address]): Option[Address] = cacheMap.getEntry(BusinessAddress)

    def getBusinessStartDate()(implicit read: Reads[BusinessStartDateModel]): Option[BusinessStartDateModel] = cacheMap.getEntry(BusinessStartDate)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getTerms()(implicit read: Reads[Boolean]): Option[Boolean] = cacheMap.getEntry(Terms)

    def getSummary()(implicit
                     isrc: Reads[IncomeSourceModel],
                     oirc: Reads[OtherIncomeModel],
                     accP: Reads[AccountingPeriodPriorModel],
                     accD: Reads[AccountingPeriodModel],
                     busName: Reads[BusinessNameModel],
                     busPhone: Reads[BusinessPhoneNumberModel],
                     busAdd: Reads[Address],
                     busStart: Reads[BusinessStartDateModel],
                     accM: Reads[AccountingMethodModel],
                     ter: Reads[Boolean]): SummaryModel = {
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
                getBusinessPhoneNumber(),
                getBusinessAddress(),
                getBusinessStartDate(),
                getAccountingMethod(),
                getTerms()
              )
          }
        case _ => SummaryModel()
      }

    }
  }

}
