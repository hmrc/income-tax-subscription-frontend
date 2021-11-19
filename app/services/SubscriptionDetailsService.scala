/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import models.common._
import models.common.business._
import models.{AccountingMethod, DateModel}
import play.api.libs.json._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionDetailsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                          (implicit ec: ExecutionContext) {

  type FO[T] = Future[Option[T]]
  type FA = Future[Any]

  private[services] def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](subscriptionId).map(_.flatMap(cache => cache.getEntry(location)))

  private[services] def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): Future[CacheMap] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](subscriptionId).flatMap { optCache =>
      val newCache = optCache match {
        case None => CacheMap("", Map(location -> Json.toJson(obj)))
        case Some(cache) => CacheMap("", cache.data.updated(location, Json.toJson(obj)))
      }
      incomeTaxSubscriptionConnector.saveSubscriptionDetails(subscriptionId, newCache) map {
        case Right(_) => newCache
        case Left(_) => CacheMap("", Map.empty[String, JsValue])
      }
    }
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[CacheMap] = incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](subscriptionId) map {
    case Some(cacheMap) => cacheMap
    case None => CacheMap("", Map.empty)
  }

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = incomeTaxSubscriptionConnector.deleteAll()

  def fetchIncomeSource()(implicit hc: HeaderCarrier): FO[IncomeSourceModel] =
    fetch[IncomeSourceModel](IncomeSource)

  def saveIncomeSource(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[IncomeSourceModel](IncomeSource, incomeSource)

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FA =
    save[BusinessNameModel](BusinessName, businessName)

  def fetchAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FO[AccountingMethodModel] =
    fetch[AccountingMethodModel](SubscriptionDataKeys.AccountingMethod)

  def fetchOverseasPropertyAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[OverseasAccountingMethodPropertyModel]): FO[OverseasAccountingMethodPropertyModel] =
    fetch[OverseasAccountingMethodPropertyModel](OverseasPropertyAccountingMethod)

  def saveAccountingMethod(accountingMethod: AccountingMethodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FA =
    save[AccountingMethodModel](SubscriptionDataKeys.AccountingMethod, accountingMethod)

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

  def fetchOverseasPropertyStartDate()(implicit hc: HeaderCarrier, reads: Reads[OverseasPropertyStartDateModel]):
  FO[OverseasPropertyStartDateModel] = fetch[OverseasPropertyStartDateModel](OverseasPropertyStartDate)

  def saveOverseasPropertyStartDate(overseasPropertyStartDate: OverseasPropertyStartDateModel)
                                   (implicit hc: HeaderCarrier, reads: Reads[OverseasPropertyStartDateModel]): FA =
    save[OverseasPropertyStartDateModel](OverseasPropertyStartDate, overseasPropertyStartDate)

  def fetchLastUpdatedTimestamp()(implicit hc: HeaderCarrier, reads: Reads[TimestampModel]): Future[Option[TimestampModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[TimestampModel](lastUpdatedTimestamp)

  def fetchProperty()(implicit hc: HeaderCarrier): Future[Option[PropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[PropertyModel](Property)

  def saveProperty(property: PropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[PropertyModel](Property, property)

  def fetchOverseasProperty()(implicit hc: HeaderCarrier): Future[Option[OverseasPropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[OverseasPropertyModel](OverseasProperty)

  def saveOverseasProperty(overseasProperty: OverseasPropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[OverseasPropertyModel](OverseasProperty, overseasProperty)

  def fetchPropertyStartDate()(implicit hc: HeaderCarrier): Future[Option[DateModel]] =
    fetchProperty().map(_.flatMap(_.startDate))

  def savePropertyStartDate(propertyStartDate: DateModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchProperty() map {
      case Some(property) => property.copy(startDate = Some(propertyStartDate), confirmed = false)
      case None => PropertyModel(startDate = Some(propertyStartDate))
    } flatMap saveProperty
  }

  def fetchAccountingMethodProperty()(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    fetchProperty().map(_.flatMap(_.accountingMethod))

  def saveAccountingMethodProperty(accountingMethod: AccountingMethod)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchProperty() map {
      case Some(property) => property.copy(accountingMethod = Some(accountingMethod), confirmed = false)
      case None => PropertyModel(accountingMethod = Some(accountingMethod))
    } flatMap saveProperty
  }

}

