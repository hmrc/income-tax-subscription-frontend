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

package core.services

import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import incometax.subscription.models.SummaryModel
import models._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap
import CacheConstants._
import incometax.business.models._
import incometax.business.models.address.Address

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap) {

    def getIncomeSource()(implicit read: Reads[IncomeSourceModel]): Option[IncomeSourceModel] = cacheMap.getEntry(IncomeSource)

    def getOtherIncome()(implicit read: Reads[OtherIncomeModel]): Option[OtherIncomeModel] = cacheMap.getEntry(OtherIncome)

    def getMatchTaxYear()(implicit read: Reads[MatchTaxYearModel]): Option[MatchTaxYearModel] = cacheMap.getEntry(MatchTaxYear)

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
                     matchT: Reads[MatchTaxYearModel],
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
                incomeSource = incomeSource,
                otherIncome = getOtherIncome(),
                matchTaxYear = getMatchTaxYear(),
                accountingPeriod = getAccountingPeriodDate(),
                businessName = getBusinessName(),
                businessPhoneNumber = getBusinessPhoneNumber(),
                businessAddress = getBusinessAddress(),
                businessStartDate = getBusinessStartDate(),
                accountingMethod = getAccountingMethod(),
                terms = getTerms()
              )
          }
        case _ => SummaryModel()
      }
    }

  }

}
