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

import common.Constants.ITSASessionKeys
import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import connectors.httpparser.RetrieveReferenceHttpParser.RetrieveReferenceResponse
import models.common._
import models.common.business._
import models.{AccountingMethod, Current, DateModel, Next}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

//scalastyle:off

@Singleton
class SubscriptionDetailsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           applicationCrypto: ApplicationCrypto)
                                          (implicit ec: ExecutionContext) {

  def deleteAll(reference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    incomeTaxSubscriptionConnector.deleteAll(reference)
  }

  def fetchPrePopFlag(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](reference, PrePopFlag)

  def savePrePopFlag(reference: String, prepop: Boolean)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Boolean](reference, PrePopFlag, prepop)

  private def getEligibilityNextYearOnlyFromSession(implicit request: Request[AnyContent]) = {
    request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).exists(_.toBoolean)
  }

  private def getMandationForCurrentYearFromSession(implicit request: Request[AnyContent]): Boolean = {
    request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).exists(_.toBoolean)
  }

  def fetchSelectedTaxYear(reference: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Option[AccountingYearModel]] = {
    if (getMandationForCurrentYearFromSession) {
      Future.successful(Some(AccountingYearModel(Current, confirmed = true, editable = false)))
    } else if (getEligibilityNextYearOnlyFromSession) {
      Future.successful(Some(AccountingYearModel(Next, confirmed = true, editable = false)))
    } else {
      incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingYearModel](reference, SubscriptionDataKeys.SelectedTaxYear).map(_.map(_.copy(editable = true)))
    }
  }

  def saveSelectedTaxYear(reference: String, selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingYearModel](reference, SelectedTaxYear, selectedTaxYear)

  def fetchLastUpdatedTimestamp(reference: String)(implicit hc: HeaderCarrier): Future[Option[TimestampModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[TimestampModel](reference, lastUpdatedTimestamp)

  def fetchProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[PropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[PropertyModel](reference, Property)

  def saveBusinesses(reference: String, selfEmploymentData: Seq[SelfEmploymentData])(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {

    def encryptBusinessList(businesses: Seq[SelfEmploymentData]): Seq[SelfEmploymentData] = {
      businesses map {
        business =>
          business.copy(
            businessName = business.businessName.map(name =>
              name.encrypt(applicationCrypto.QueryParameterCrypto)
            ),
            businessAddress = business.businessAddress.map(address =>
              address.encrypt(applicationCrypto.QueryParameterCrypto)
            )
          )
      }
    }

    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey, encryptBusinessList(selfEmploymentData))
  }

  def saveProperty(reference: String, property: PropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[PropertyModel](reference, Property, property)

  def fetchOverseasProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[OverseasPropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[OverseasPropertyModel](reference, SubscriptionDataKeys.OverseasProperty)

  def fetchSoftwareFlag(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    //todo: implement correct fetch when page is implemented, for now, return Some(true)
    Future.successful(Some(true))
  }

  def fetchIncomeSourcesConfirmation(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](reference, SubscriptionDataKeys.IncomeSourceConfirmation)
  }

  def saveIncomeSourcesConfirmation(reference: String)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Boolean](reference, SubscriptionDataKeys.IncomeSourceConfirmation, true)
  }

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

  def fetchAllSelfEmployments(reference: String)(implicit hc: HeaderCarrier): Future[Seq[SelfEmploymentData]] = {

    def decryptBusinessList(businesses: Seq[SelfEmploymentData]): Seq[SelfEmploymentData] = {
      businesses map {
        business =>
          business.copy(
            businessName = business.businessName.map(name =>
              BusinessNameModel(applicationCrypto.QueryParameterCrypto.decrypt(Crypted(name.businessName)).value)
            ),
            businessAddress = business.businessAddress.map(address =>
              address.copy(
                address = Address(
                  lines = address.address.lines.map(
                    line => applicationCrypto.QueryParameterCrypto.decrypt(Crypted(line)).value
                  ),
                  postcode = address.address.postcode.map(
                    postcode => applicationCrypto.QueryParameterCrypto.decrypt(Crypted(postcode)).value
                  )
                )
              )
            )
          )
      }
    }

    incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey).map(decryptBusinessList)
  }

  def fetchSelfEmploymentsAccountingMethod(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingMethod]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      .map(_.map(_.accountingMethod))

  def saveSelfEmploymentsAccountingMethod(reference: String, accountingMethodModel: AccountingMethodModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod, accountingMethodModel)

  def fetchAllIncomeSources(reference: String)(implicit hc: HeaderCarrier): Future[IncomeSources] = {
    for {
      selfEmployments <- fetchAllSelfEmployments(reference)
      ukProperty <- fetchProperty(reference)
      foreignProperty <- fetchOverseasProperty(reference)
    } yield {
      IncomeSources(
        selfEmployments,
        ukProperty,
        foreignProperty
      )
    }
  }
}