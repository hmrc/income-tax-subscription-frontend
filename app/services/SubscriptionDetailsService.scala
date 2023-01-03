/*
 * Copyright 2023 HM Revenue & Customs
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
import models.status.MandationStatusModel
import models.{AccountingMethod, DateModel}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

//scalastyle:off

@Singleton
class SubscriptionDetailsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                          (implicit ec: ExecutionContext) {

  def deleteAll(reference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    incomeTaxSubscriptionConnector.deleteAll(reference)
  }

  def fetchPrePopFlag(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](reference, PrePopFlag)

  def savePrePopFlag(reference: String, prepop: Boolean)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Boolean](reference, PrePopFlag, prepop)

  def fetchBusinessName(reference: String)(implicit hc: HeaderCarrier): Future[Option[BusinessNameModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[BusinessNameModel](reference, SubscriptionDataKeys.BusinessName)

  def fetchSelectedTaxYear(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingYearModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingYearModel](reference, SubscriptionDataKeys.SelectedTaxYear)

  def saveSelectedTaxYear(reference: String, selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingYearModel](reference, SelectedTaxYear, selectedTaxYear)

  def fetchSubscriptionId(reference: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[String](reference, MtditId)

  def saveSubscriptionId(reference: String, mtditId: String)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[String](reference, MtditId, mtditId)

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

  def fetchAllSelfEmployments(reference: String)(implicit hc: HeaderCarrier): Future[Seq[SelfEmploymentData]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)

  def fetchSelfEmploymentsAccountingMethod(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      .map(_.map(_.accountingMethod))

  def saveSelfEmploymentsAccountingMethod(reference: String, accountingMethodModel: AccountingMethodModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod, accountingMethodModel)

  def saveMandationStatus(reference: String, mandationStatus: MandationStatusModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[MandationStatusModel](reference, MandationStatus, mandationStatus)

}

