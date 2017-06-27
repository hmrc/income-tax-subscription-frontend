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


import javax.inject._

import models._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class KeystoreService @Inject()(val session: SessionCache) {

  type FO[T] = Future[Option[T]]
  type FC = Future[CacheMap]

  protected def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] = session.fetchAndGetEntry(location)

  protected def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): FC = session.cache(location, obj)

  def fetchAll()(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = session.fetch()

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = session.remove()

  import CacheConstants._

  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FO[IncomeSourceModel] =
    fetch[IncomeSourceModel](IncomeSource)

  def saveIncomeSource(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FC =
    save[IncomeSourceModel](IncomeSource, incomeSource)

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FC =
    save[BusinessNameModel](BusinessName, businessName)

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

  def fetchNotEligible()(implicit hc: HeaderCarrier, reads: Reads[NotEligibleModel]): FO[NotEligibleModel] =
    fetch[NotEligibleModel](NotEligible)

  def saveNotEligible(choice: NotEligibleModel)(implicit hc: HeaderCarrier, reads: Reads[NotEligibleModel]): FC =
    save[NotEligibleModel](NotEligible, choice)

  def fetchSubscriptionId()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] = fetch[String](MtditId)

  def saveSubscriptionId(mtditId: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC = save[String](MtditId, mtditId)

  def fetchAccountingPeriodPrior()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodPriorModel]): FO[AccountingPeriodPriorModel] =
    fetch[AccountingPeriodPriorModel](AccountingPeriodPrior)

  def saveAccountingPeriodPrior(accountingPeriodPrior: AccountingPeriodPriorModel)
                               (implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodPriorModel]): FC =
    save[AccountingPeriodPriorModel](AccountingPeriodPrior, accountingPeriodPrior)
}

