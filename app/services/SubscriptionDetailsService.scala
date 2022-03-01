/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpparser.RetrieveReferenceHttpParser.RetrieveReferenceResponse
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

//scalastyle:off

@Singleton
class SubscriptionDetailsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                          (implicit ec: ExecutionContext) {

  private[services] def fetch[T](reference: String, location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Option[T]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](reference, subscriptionId).map(_.flatMap(cache => cache.getEntry(location)))

  private[services] def save[T](reference: String, location: String, obj: T)
                               (implicit hc: HeaderCarrier, reads: Writes[T]): Future[CacheMap] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](reference, subscriptionId).flatMap { optCache =>
      val newCache = optCache match {
        case None => CacheMap("", Map(location -> Json.toJson(obj)))
        case Some(cache) => CacheMap("", cache.data.updated(location, Json.toJson(obj)))
      }
      incomeTaxSubscriptionConnector.saveSubscriptionDetails(reference, subscriptionId, newCache) map {
        case Right(_) => newCache
        case Left(_) => CacheMap("", Map.empty[String, JsValue])
      }
    }
  }

  def fetchAll(reference: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[CacheMap](reference, subscriptionId) map {
      case Some(cacheMap) => cacheMap
      case None => CacheMap("", Map.empty)
    }
  }

  def deleteAll(reference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    incomeTaxSubscriptionConnector.deleteAll(reference)
  }

  def fetchPrePopFlag(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    fetch[Boolean](reference, PrePopFlag)

  def savePrePopFlag(reference: String, prepop: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[Boolean](reference, PrePopFlag, prepop)

  def fetchIncomeSource(reference: String)(implicit hc: HeaderCarrier): Future[Option[IncomeSourceModel]] =
    fetch[IncomeSourceModel](reference, IncomeSource)

  def saveIncomeSource(reference: String, incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[IncomeSourceModel](reference, IncomeSource, incomeSource)

  def fetchBusinessName(reference: String)(implicit hc: HeaderCarrier): Future[Option[BusinessNameModel]] =
    fetch[BusinessNameModel](reference, BusinessName)

  def saveBusinessName(reference: String, businessName: BusinessNameModel)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[BusinessNameModel](reference, BusinessName, businessName)

  def fetchSelectedTaxYear(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingYearModel]] =
    fetch[AccountingYearModel](reference, SelectedTaxYear)

  def saveSelectedTaxYear(reference: String, selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[AccountingYearModel](reference, SelectedTaxYear, selectedTaxYear)

  def fetchSubscriptionId(reference: String)(implicit hc: HeaderCarrier): Future[Option[String]] = fetch[String](reference, MtditId)

  def saveSubscriptionId(reference: String, mtditId: String)(implicit hc: HeaderCarrier): Future[CacheMap] = save[String](reference, MtditId, mtditId)

  def fetchPaperlessPreferenceToken(reference: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    fetch[String](reference, PaperlessPreferenceToken)

  def savePaperlessPreferenceToken(reference: String, token: String)(implicit hc: HeaderCarrier): Future[CacheMap] =
    save[String](reference, PaperlessPreferenceToken, token)

  def fetchLastUpdatedTimestamp(reference: String)(implicit hc: HeaderCarrier): Future[Option[TimestampModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[TimestampModel](reference, lastUpdatedTimestamp)

  def fetchProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[PropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[PropertyModel](reference, Property)

  def saveBusinesses(reference: String, selfEmploymentData: Seq[SelfEmploymentData])(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey, selfEmploymentData)

  def saveProperty(reference: String, property: PropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[PropertyModel](reference, Property, property)

  def fetchOverseasProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[OverseasPropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[OverseasPropertyModel](reference, SubscriptionDataKeys.OverseasProperty)

  def saveOverseasProperty(reference: String, overseasProperty: OverseasPropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[OverseasPropertyModel](reference, SubscriptionDataKeys.OverseasProperty, overseasProperty)

  def fetchPropertyStartDate(reference: String)(implicit hc: HeaderCarrier): Future[Option[DateModel]] =
    fetchProperty(reference).map(_.flatMap(_.startDate))

  def savePropertyStartDate(reference: String, propertyStartDate: DateModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchProperty(reference) map {
      case Some(property) => property.copy(startDate = Some(propertyStartDate), confirmed = false)
      case None => PropertyModel(startDate = Some(propertyStartDate))
    } flatMap { model =>
      saveProperty(reference, model)
    }
  }

  def fetchAccountingMethodProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    fetchProperty(reference).map(_.flatMap(_.accountingMethod))

  def saveAccountingMethodProperty(reference: String, accountingMethod: AccountingMethod)
                                  (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchProperty(reference) map {
      case Some(property) => property.copy(accountingMethod = Some(accountingMethod), confirmed = false)
      case None => PropertyModel(accountingMethod = Some(accountingMethod))
    } flatMap { model =>
      saveProperty(reference, model)
    }
  }

  def retrieveReference(utr: String)(implicit hc: HeaderCarrier): Future[RetrieveReferenceResponse] = {
    incomeTaxSubscriptionConnector.retrieveReference(utr)
  }

  def fetchOverseasPropertyStartDate(reference: String)(implicit hc: HeaderCarrier): Future[Option[DateModel]] =
    fetchOverseasProperty(reference).map(_.flatMap(_.startDate))

  def saveOverseasPropertyStartDate(reference: String, propertyStartDate: DateModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchOverseasProperty(reference) map {
      case Some(property) => property.copy(startDate = Some(propertyStartDate), confirmed = false)
      case None => OverseasPropertyModel(startDate = Some(propertyStartDate))
    } flatMap { model =>
      saveOverseasProperty(reference, model)
    }
  }

  def fetchOverseasPropertyAccountingMethod(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    fetchOverseasProperty(reference).map(_.flatMap(_.accountingMethod))

  def saveOverseasAccountingMethodProperty(reference: String, accountingMethod: AccountingMethod)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchOverseasProperty(reference) map {
      case Some(property) => property.copy(accountingMethod = Some(accountingMethod), confirmed = false)
      case None => OverseasPropertyModel(accountingMethod = Some(accountingMethod))
    } flatMap { model =>
      saveOverseasProperty(reference, model)
    }
  }

  def fetchAllSelfEmployments(reference: String)(implicit hc: HeaderCarrier): Future[Option[Seq[SelfEmploymentData]]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey)

  def fetchSelfEmploymentsAccountingMethod(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      .map(_.map(_.accountingMethod))

  def saveSelfEmploymentsAccountingMethod(reference: String, accountingMethodModel: AccountingMethodModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod, accountingMethodModel)

}

