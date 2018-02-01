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

import core.config.AppConfig
import core.services.CacheConstants._
import incometax.business.models._
import incometax.business.models.address.Address
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.models._
import incometax.subscription.models._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.util.Try

object CacheUtil {

  implicit class CacheMapUtil(cacheMap: CacheMap)(implicit appConfig: AppConfig) {

    //TODO remove when we switch to the new income source flow
    def getIncomeSource()(implicit read: Reads[IncomeSourceModel]): Option[IncomeSourceModel] = cacheMap.getEntry(IncomeSource)

    def getRentUkProperty()(implicit read: Reads[RentUkPropertyModel]): Option[RentUkPropertyModel] = cacheMap.getEntry(RentUkProperty)

    def getWorkForYourself()(implicit read: Reads[WorkForYourselfModel]): Option[WorkForYourselfModel] = cacheMap.getEntry(WorkForYourself)

    def getIncomeSourceType()(implicit read: Reads[IncomeSourceModel], readR: Reads[RentUkPropertyModel],
                              readW: Reads[WorkForYourselfModel]): Option[IncomeSourceType] =
      if (appConfig.newIncomeSourceFlowEnabled)
        getRentUkProperty().flatMap(rentUkProperty => IncomeSourceType.from(rentUkProperty, getWorkForYourself()))
      else
        getIncomeSource().map(incSrc => IncomeSourceType(incSrc.source))

    def getOtherIncome()(implicit read: Reads[OtherIncomeModel]): Option[OtherIncomeModel] = cacheMap.getEntry(OtherIncome)

    def getMatchTaxYear()(implicit read: Reads[MatchTaxYearModel]): Option[MatchTaxYearModel] = cacheMap.getEntry(MatchTaxYear)

    def getAccountingPeriodDate()(implicit read: Reads[AccountingPeriodModel]): Option[AccountingPeriodModel] = cacheMap.getEntry(AccountingPeriodDate)

    def getBusinessName()(implicit read: Reads[BusinessNameModel]): Option[BusinessNameModel] = cacheMap.getEntry(BusinessName)

    def getBusinessPhoneNumber()(implicit read: Reads[BusinessPhoneNumberModel]): Option[BusinessPhoneNumberModel] = cacheMap.getEntry(BusinessPhoneNumber)

    def getBusinessAddress()(implicit read: Reads[Address]): Option[Address] = cacheMap.getEntry(BusinessAddress)

    def getBusinessStartDate()(implicit read: Reads[BusinessStartDateModel]): Option[BusinessStartDateModel] = cacheMap.getEntry(BusinessStartDate)

    def getAccountingMethod()(implicit read: Reads[AccountingMethodModel]): Option[AccountingMethodModel] = cacheMap.getEntry(AccountingMethod)

    def getTerms()(implicit read: Reads[Boolean]): Option[Boolean] = cacheMap.getEntry(Terms)

    //TODO update when we switch to the new income source flow
    def getSummary()(implicit appConfig: AppConfig,
                     isrc: Reads[IncomeSourceModel],
                     wfyrc: Reads[WorkForYourselfModel],
                     oirc: Reads[OtherIncomeModel],
                     matchT: Reads[MatchTaxYearModel],
                     accD: Reads[AccountingPeriodModel],
                     busName: Reads[BusinessNameModel],
                     busPhone: Reads[BusinessPhoneNumberModel],
                     busAdd: Reads[Address],
                     busStart: Reads[BusinessStartDateModel],
                     accM: Reads[AccountingMethodModel],
                     ter: Reads[Boolean]): SummaryModel =
      if (appConfig.newIncomeSourceFlowEnabled) {
        getIncomeSourceType() match {
          case Some(Property) =>
            SummaryModel(
              rentUkProperty = getRentUkProperty(),
              workForYourself = getWorkForYourself(),
              otherIncome = getOtherIncome(),
              terms = getTerms()
            )
          case Some(incomeSourceType@_) =>
            SummaryModel(
              rentUkProperty = getRentUkProperty(),
              workForYourself = getWorkForYourself(),
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
          case _ => SummaryModel()
        }
      }
      else {
        val incomeSource = getIncomeSource()
        incomeSource match {
          case Some(src) =>
            src.source match {
              case IncomeSourceForm.option_property =>
                SummaryModel(
                  incomeSource,
                  otherIncome = getOtherIncome(),
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
