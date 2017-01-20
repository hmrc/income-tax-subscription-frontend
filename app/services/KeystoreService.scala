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


import config.SessionCache
import models._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

trait KeystoreService {

  type FO[T] = Future[Option[T]]
  type FC = Future[CacheMap]

  protected val session: SessionCache

  protected def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] = session.fetchAndGetEntry(location)

  protected def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): FC = session.cache(location, obj)

  def fetchAll()(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = session.fetch()

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = session.remove()

  import CacheConstants._

  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FO[IncomeSourceModel] =
    fetch[IncomeSourceModel](IncomeSource)

  def saveIncomeSource(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FC =
    save[IncomeSourceModel](IncomeSource, incomeSource)

  def fetchPropertyIncome()(implicit hc: HeaderCarrier, reads: Reads[PropertyIncomeModel]): FO[PropertyIncomeModel] =
    fetch[PropertyIncomeModel](PropertyIncome)

  def savePropertyIncome(propertyIncome: PropertyIncomeModel)(implicit hc: HeaderCarrier, reads: Reads[PropertyIncomeModel]): FC =
    save[PropertyIncomeModel](PropertyIncome, propertyIncome)

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FC =
    save[BusinessNameModel](BusinessName, businessName)

  def fetchAccountingPeriod()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FO[AccountingPeriodModel] =
    fetch[AccountingPeriodModel](AccountingPeriod)

  def saveAccountingPeriod(accountingPeriod: AccountingPeriodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FC =
    save[AccountingPeriodModel](AccountingPeriod, accountingPeriod)

  def fetchContactEmail()(implicit hc: HeaderCarrier, reads: Reads[EmailModel]): FO[EmailModel] =
    fetch[EmailModel](ContactEmail)

  def saveContactEmail(contactEmail: EmailModel)(implicit hc: HeaderCarrier, reads: Reads[EmailModel]): FC =
    save[EmailModel](ContactEmail, contactEmail)

  def fetchIncomeType()(implicit hc: HeaderCarrier, reads: Reads[IncomeTypeModel]): FO[IncomeTypeModel] =
    fetch[IncomeTypeModel](IncomeType)

  def saveIncomeType(incomeType: IncomeTypeModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeTypeModel]): FC =
    save[IncomeTypeModel](IncomeType, incomeType)

  def fetchTerms()(implicit hc: HeaderCarrier, reads: Reads[TermModel]): FO[TermModel] =
    fetch[TermModel](Terms)

  def saveTerms(terms: TermModel)(implicit hc: HeaderCarrier, reads: Reads[TermModel]): FC =
    save[TermModel](Terms, terms)

  def fetchNotEligible()(implicit hc: HeaderCarrier, reads: Reads[NotEligibleModel]): FO[NotEligibleModel] =
    fetch[NotEligibleModel](NotEligible)

  def saveNotEligible(choice: NotEligibleModel)(implicit hc: HeaderCarrier, reads: Reads[NotEligibleModel]): FC =
    save[NotEligibleModel](NotEligible, choice)
}

object KeystoreService extends KeystoreService {
  val session = SessionCache
}
