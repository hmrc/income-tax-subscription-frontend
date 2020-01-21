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

package core.services

import core.models.YesNo
import incometax.business.models._
import incometax.business.models.address.Address
import incometax.incomesource.models.{AreYouSelfEmployedModel, RentUkPropertyModel}
import incometax.subscription.models.IncomeSourceType
import javax.inject._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class KeystoreService @Inject()(val session: SessionCache) {

  type FO[T] = Future[Option[T]]
  type FC = Future[CacheMap]

  protected def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] = session.fetchAndGetEntry(location)

  protected def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): FC = session.cache(location, obj) recoverWith {
    case ex => Future.failed(new InternalServerException(ex.getMessage))
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[CacheMap] = session.fetch() map {
    case Some(cacheMap) => cacheMap
    case None => CacheMap("", Map.empty)
  }

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = session.remove()

  import CacheConstants._


  def fetchAreYouSelfEmployed()(implicit hc: HeaderCarrier, reads: Reads[AreYouSelfEmployedModel]): FO[AreYouSelfEmployedModel] =
    fetch[AreYouSelfEmployedModel](AreYouSelfEmployed)

  def saveAreYouSelfEmployed(areYouSelfEmployed: AreYouSelfEmployedModel)(implicit hc: HeaderCarrier, reads: Reads[AreYouSelfEmployedModel]): FC =
    save[AreYouSelfEmployedModel](AreYouSelfEmployed, areYouSelfEmployed)

  //TODO remove when we switch to the new income source flow
  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FO[IncomeSourceType] =
    fetch[IncomeSourceType](IncomeSource)

  //TODO remove when we switch to the new income source flow
  def saveIncomeSource(incomeSource: IncomeSourceType)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FC =
    save[IncomeSourceType](IncomeSource, incomeSource)

  def fetchRentUkProperty()(implicit hc: HeaderCarrier, reads: Reads[RentUkPropertyModel]): FO[RentUkPropertyModel] =
    fetch[RentUkPropertyModel](RentUkProperty)

  def saveRentUkProperty(rentUkPropertyModel: RentUkPropertyModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FC =
    save[RentUkPropertyModel](RentUkProperty, rentUkPropertyModel)

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FC =
    save[BusinessNameModel](BusinessName, businessName)

  def fetchBusinessPhoneNumber()(implicit hc: HeaderCarrier, reads: Reads[BusinessPhoneNumberModel]): FO[BusinessPhoneNumberModel] =
    fetch[BusinessPhoneNumberModel](BusinessPhoneNumber)

  def saveBusinessPhoneNumber(businessPhoneNumber: BusinessPhoneNumberModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessPhoneNumberModel]): FC =
    save[BusinessPhoneNumberModel](BusinessPhoneNumber, businessPhoneNumber)

  def fetchBusinessAddress()(implicit hc: HeaderCarrier, reads: Reads[Address]): FO[Address] =
    fetch[Address](BusinessAddress)

  def saveBusinessAddress(address: Address)(implicit hc: HeaderCarrier, reads: Reads[Address]): FC =
    save[Address](BusinessAddress, address)

  def fetchBusinessStartDate()(implicit hc: HeaderCarrier, reads: Reads[BusinessStartDateModel]): FO[BusinessStartDateModel] =
    fetch[BusinessStartDateModel](BusinessStartDate)

  def saveBusinessStartDate(businessStartDate: BusinessStartDateModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessStartDateModel]): FC =
    save[BusinessStartDateModel](BusinessStartDate, businessStartDate)

  def fetchMatchTaxYear()(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FO[MatchTaxYearModel] =
    fetch[MatchTaxYearModel](MatchTaxYear)

  def saveMatchTaxYear(accountingPeriod: MatchTaxYearModel)(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FC =
    save[MatchTaxYearModel](MatchTaxYear, accountingPeriod)

  def fetchAccountingPeriodDate()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FO[AccountingPeriodModel] =
    fetch[AccountingPeriodModel](AccountingPeriodDate)

  def saveAccountingPeriodDate(accountingPeriod: AccountingPeriodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FC =
    save[AccountingPeriodModel](AccountingPeriodDate, accountingPeriod)

  def fetchAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FO[AccountingMethodModel] =
    fetch[AccountingMethodModel](AccountingMethod)

  def saveAccountingMethod(accountingMethod: AccountingMethodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FC =
    save[AccountingMethodModel](AccountingMethod, accountingMethod)

  def fetchAccountingMethodProperty()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FO[AccountingMethodPropertyModel] =
    fetch[AccountingMethodPropertyModel](PropertyAccountingMethod)

  def saveAccountingMethodProperty(accountingMethodProperty: AccountingMethodPropertyModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FC =
    save[AccountingMethodPropertyModel](PropertyAccountingMethod, accountingMethodProperty)

  def fetchSelectedTaxYear()(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FO[AccountingYearModel] =
    fetch[AccountingYearModel](SelectedTaxYear)

  def saveSelectedTaxYear(selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FC =
    save[AccountingYearModel](SelectedTaxYear, selectedTaxYear)

  def fetchSubscriptionId()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] = fetch[String](MtditId)

  def saveSubscriptionId(mtditId: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC = save[String](MtditId, mtditId)

  def fetchPaperlessPreferenceToken()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] =
    fetch[String](PaperlessPreferenceToken)

  def savePaperlessPreferenceToken(token: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC =
    save[String](PaperlessPreferenceToken, token)
}

