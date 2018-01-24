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

import javax.inject._

import incometax.business.models._
import incometax.business.models.address.Address
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel, WorkForYourselfModel}
import models._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import usermatching.models.UserDetailsModel

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


  def fetchWorkForYourself()(implicit hc: HeaderCarrier, reads: Reads[WorkForYourselfModel]): FO[WorkForYourselfModel] =
    fetch[WorkForYourselfModel](WorkForYourself)

  def saveWorkForYourself(workForYourself: WorkForYourselfModel)(implicit hc: HeaderCarrier, reads: Reads[WorkForYourselfModel]): FC =
    save[WorkForYourselfModel](WorkForYourself, workForYourself)

  //TODO remove when we switch to the new income source flow
  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FO[IncomeSourceModel] =
    fetch[IncomeSourceModel](IncomeSource)

  //TODO remove when we switch to the new income source flow
  def saveIncomeSource(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FC =
    save[IncomeSourceModel](IncomeSource, incomeSource)

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

  def fetchTerms()(implicit hc: HeaderCarrier, reads: Reads[Boolean]): FO[Boolean] =
    fetch[Boolean](Terms)

  def saveTerms(terms: Boolean)(implicit hc: HeaderCarrier, reads: Reads[Boolean]): FC =
    save[Boolean](Terms, terms)

  def fetchOtherIncome()(implicit hc: HeaderCarrier, reads: Reads[OtherIncomeModel]): FO[OtherIncomeModel] =
    fetch[OtherIncomeModel](OtherIncome)

  def saveOtherIncome(otherIncome: OtherIncomeModel)(implicit hc: HeaderCarrier, reads: Reads[OtherIncomeModel]): FC =
    save[OtherIncomeModel](OtherIncome, otherIncome)

  def fetchSubscriptionId()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] = fetch[String](MtditId)

  def saveSubscriptionId(mtditId: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC = save[String](MtditId, mtditId)

  def fetchPaperlessPreferenceToken()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] =
    fetch[String](PaperlessPreferenceToken)

  def savePaperlessPreferenceToken(token: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC =
    save[String](PaperlessPreferenceToken, token)
}

