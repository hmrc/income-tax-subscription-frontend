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

package services

import connectors.IncomeTaxSubscriptionConnector
import javax.inject._
import models.common._
import models.individual.business._
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription.IncomeSourceType
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.SubscriptionDataKeys._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionDetailsService @Inject()(val subscriptionDetailsSession: IncomeTaxSubscriptionConnector)
                                          (implicit ec: ExecutionContext) {

  type FO[T] = Future[Option[T]]
  type FA = Future[Any]

  private[services] def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] =
    subscriptionDetailsSession.getSubscriptionDetails[CacheMap](subscriptionId).map(_.flatMap(cache => cache.getEntry(location)))

  private[services] def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): FA = {
    subscriptionDetailsSession.getSubscriptionDetails[CacheMap](subscriptionId).map { optCache =>
      val newCache = optCache match {
        case None => CacheMap("", Map(location -> Json.toJson(obj)))
        case Some(cache) => CacheMap("", cache.data.updated(location, Json.toJson(obj)))
      }
      subscriptionDetailsSession.saveSubscriptionDetails(subscriptionId, newCache)
    }
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[CacheMap] = subscriptionDetailsSession.getSubscriptionDetails[CacheMap](subscriptionId) map {
    case Some(cacheMap) => cacheMap
    case None => CacheMap("", Map.empty)
  }

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = subscriptionDetailsSession.deleteAll()

  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FO[IncomeSourceType] =
    fetch[IncomeSourceType](IncomeSource)

  def saveIncomeSource(incomeSource: IncomeSourceType)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FA =
    save[IncomeSourceType](IncomeSource, incomeSource)

  def fetchIndividualIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FO[IncomeSourceModel] =
    fetch[IncomeSourceModel](IndividualIncomeSource)

  def saveIndividualIncomeSource(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceModel]): FA =
    save[IncomeSourceModel](IndividualIncomeSource, incomeSource)


  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FA =
    save[BusinessNameModel](BusinessName, businessName)

  def fetchMatchTaxYear()(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FO[MatchTaxYearModel] =
    fetch[MatchTaxYearModel](MatchTaxYear)

  def saveMatchTaxYear(accountingPeriod: MatchTaxYearModel)(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FA =
    save[MatchTaxYearModel](MatchTaxYear, accountingPeriod)

  def fetchAccountingPeriodDate()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FO[AccountingPeriodModel] =
    fetch[AccountingPeriodModel](AccountingPeriodDate)

  def saveAccountingPeriodDate(accountingPeriod: AccountingPeriodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FA =
    save[AccountingPeriodModel](AccountingPeriodDate, accountingPeriod)

  def fetchAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FO[AccountingMethodModel] =
    fetch[AccountingMethodModel](AccountingMethod)

  def fetchOverseasPropertyAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[OverseasAccountingMethodPropertyModel]): FO[OverseasAccountingMethodPropertyModel] =
    fetch[OverseasAccountingMethodPropertyModel](OverseasPropertyAccountingMethod)

  def saveAccountingMethod(accountingMethod: AccountingMethodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FA =
    save[AccountingMethodModel](AccountingMethod, accountingMethod)

  def fetchAccountingMethodProperty()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FO[AccountingMethodPropertyModel] =
    fetch[AccountingMethodPropertyModel](PropertyAccountingMethod)

  def saveAccountingMethodProperty(accountingMethodProperty: AccountingMethodPropertyModel)
                                  (implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FA = save[AccountingMethodPropertyModel](
    PropertyAccountingMethod, accountingMethodProperty)

  def saveOverseasAccountingMethodProperty(overseasPropertyAccountingMethod: OverseasAccountingMethodPropertyModel)
                                  (implicit hc: HeaderCarrier, reads: Reads[OverseasAccountingMethodPropertyModel]): FA = save[OverseasAccountingMethodPropertyModel](
    OverseasPropertyAccountingMethod, overseasPropertyAccountingMethod)

  def fetchSelectedTaxYear()(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FO[AccountingYearModel] =
    fetch[AccountingYearModel](SelectedTaxYear)

  def saveSelectedTaxYear(selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FA =
    save[AccountingYearModel](SelectedTaxYear, selectedTaxYear)

  def fetchSubscriptionId()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] = fetch[String](MtditId)

  def saveSubscriptionId(mtditId: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FA = save[String](MtditId, mtditId)

  def fetchPaperlessPreferenceToken()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] =
    fetch[String](PaperlessPreferenceToken)

  def savePaperlessPreferenceToken(token: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FA =
    save[String](PaperlessPreferenceToken, token)

  def fetchPropertyCommencementDate()(implicit hc: HeaderCarrier, reads: Reads[PropertyCommencementDateModel]): FO[PropertyCommencementDateModel] =
    fetch[PropertyCommencementDateModel](PropertyCommencementDate)

  def savePropertyCommencementDate(propertyCommencementDate: PropertyCommencementDateModel)
                                  (implicit hc: HeaderCarrier, reads: Reads[PropertyCommencementDateModel]): FA =
    save[PropertyCommencementDateModel](PropertyCommencementDate, propertyCommencementDate)

  def fetchOverseasPropertyCommencementDate()(implicit hc: HeaderCarrier, reads: Reads[OverseasPropertyCommencementDateModel]):
    FO[OverseasPropertyCommencementDateModel] = fetch[OverseasPropertyCommencementDateModel](OverseasPropertyCommencementDate)

  def saveOverseasPropertyCommencementDate(foreignPropertyCommencementDate: OverseasPropertyCommencementDateModel)
                                          (implicit hc: HeaderCarrier, reads: Reads[OverseasPropertyCommencementDateModel]): FA =
    save[OverseasPropertyCommencementDateModel](OverseasPropertyCommencementDate, foreignPropertyCommencementDate)
}

