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
import connectors.httpparser.{DeleteSubscriptionDetailsHttpParser, PostSubscriptionDetailsHttpParser}
import models._
import models.common._
import models.common.business._
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

//scalastyle:off

@Singleton
class SubscriptionDetailsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           mandationStatusService: MandationStatusService,
                                           eligibilityStatusService: GetEligibilityStatusService,
                                           applicationCrypto: ApplicationCrypto)
                                          (implicit ec: ExecutionContext) {

  implicit val jsonCrypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto

  def deleteAll(reference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    incomeTaxSubscriptionConnector.deleteAll(reference)
  }

  def fetchPrePopFlag(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](reference, PrePopFlag)

  def savePrePopFlag(reference: String, prepop: Boolean)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Boolean](reference, PrePopFlag, prepop)

  private def getEligibilityNextYearOnly(implicit hc: HeaderCarrier): Future[Boolean] = {
    eligibilityStatusService.getEligibilityStatus.map(_.eligibleNextYearOnly)
  }

  private def getMandationForCurrentYear(implicit hc: HeaderCarrier): Future[Boolean] = {
    mandationStatusService.getMandationStatus.map(_.currentYearStatus.isMandated)
  }

  def fetchSelectedTaxYear(reference: String)(implicit hc: HeaderCarrier): Future[Option[AccountingYearModel]] = {
    for {
      storedTaxYear <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingYearModel](reference, SubscriptionDataKeys.SelectedTaxYear)
      mandatedCurrentYear <- getMandationForCurrentYear
      eligibleNextYearOnly <- getEligibilityNextYearOnly
    } yield {
      if (mandatedCurrentYear) {
        Some(AccountingYearModel(Current, confirmed = true, editable = false))
      } else if (eligibleNextYearOnly) {
        Some(AccountingYearModel(Next, confirmed = true, editable = false))
      } else {
        storedTaxYear
      }
    }
  }

  def saveSelectedTaxYear(reference: String, selectedTaxYear: AccountingYearModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[AccountingYearModel](reference, SelectedTaxYear, selectedTaxYear)

  def fetchAccountingPeriod(reference:String)(implicit hc: HeaderCarrier): Future[Option[BusinessAccountingPeriod]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[BusinessAccountingPeriod](reference, AccountingPeriod)
  }

  def saveAccountingPeriod(reference: String, accountingPeriod: BusinessAccountingPeriod)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[BusinessAccountingPeriod](reference, AccountingPeriod, accountingPeriod)

  def fetchLastUpdatedTimestamp(reference: String)(implicit hc: HeaderCarrier): Future[Option[TimestampModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[TimestampModel](reference, lastUpdatedTimestamp)

  def fetchProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[PropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[PropertyModel](reference, Property)

  private def taskListStatusUpdate(reference: String, connector: IncomeTaxSubscriptionConnector, result: PostSubscriptionDetailsResponse)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    result match {
      case Right(value) =>
        connector.deleteSubscriptionDetails(
          reference = reference,
          key = SubscriptionDataKeys.IncomeSourceConfirmation
        ) map {
          case Right(_) => Right(value)
          case Left(DeleteSubscriptionDetailsHttpParser.UnexpectedStatusFailure(status)) => Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(status))
        }
      case Left(value) => Future.successful(Left(value))
    }
  }

  def saveBusinesses(reference: String, selfEmploymentData: Seq[SelfEmploymentData], accountingMethod: Option[AccountingMethod])
                    (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    val soleTraderBusinesses = SoleTraderBusinesses(
      businesses = selfEmploymentData.map { se =>
        SoleTraderBusiness(
          id = se.id,
          confirmed = se.confirmed,
          startDateBeforeLimit = se.startDateBeforeLimit,
          startDate = se.businessStartDate.map(_.startDate),
          name = se.businessName.map(_.businessName),
          trade = se.businessTradeName.map(_.businessTradeName),
          address = se.businessAddress.map(_.address).map(address => EncryptingAddress(address.lines, address.postcode))
        )
      },
      accountingMethod = accountingMethod
    )
    incomeTaxSubscriptionConnector.saveSubscriptionDetails(reference, SoleTraderBusinessesKey, soleTraderBusinesses)(implicitly, SoleTraderBusinesses.encryptedFormat).flatMap {
      result =>
        taskListStatusUpdate(reference, incomeTaxSubscriptionConnector, result)
    }
  }

  def saveProperty(reference: String, property: PropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[PropertyModel](reference, Property, property).flatMap {
      result =>
        taskListStatusUpdate(reference, incomeTaxSubscriptionConnector, result)
    }

  def saveStreamlineProperty(reference: String,
                             maybeStartDate: Option[DateModel],
                             maybeStartDateBeforeLimit: Option[Boolean],
                             accountingMethod: AccountingMethod)
                            (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchProperty(reference) map {
      case Some(property) => property
      case None => PropertyModel()
    } flatMap { propertyModel =>
      val updatedPropertyModel = maybeStartDate match {
        case Some(startDate) => propertyModel.copy(startDate = Some(startDate), accountingMethod = Some(accountingMethod), confirmed = false)
        case None => propertyModel.copy(startDateBeforeLimit = maybeStartDateBeforeLimit, accountingMethod = Some(accountingMethod), confirmed = false)
      }
      saveProperty(reference, updatedPropertyModel)
    }
  }

  def saveStreamlineForeignProperty(reference: String,
                                    maybeStartDate: Option[DateModel],
                                    maybeStartDateBeforeLimit: Option[Boolean],
                                    accountingMethod: AccountingMethod)
                                   (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    fetchOverseasProperty(reference) map {
      case Some(overseasProperty) => overseasProperty
      case None => OverseasPropertyModel()
    } flatMap { overseasPropertyModel =>
      val updatedOverseasPropertyModel = maybeStartDate match {
        case Some(startDate) => overseasPropertyModel.copy(startDate = Some(startDate), accountingMethod = Some(accountingMethod), confirmed = false)
        case None => overseasPropertyModel.copy(startDateBeforeLimit = maybeStartDateBeforeLimit, accountingMethod = Some(accountingMethod), confirmed = false)
      }
      saveOverseasProperty(reference, updatedOverseasPropertyModel)
    }
  }

  def fetchPropertyStartDateBeforeLimit(reference: String)(implicit hc: HeaderCarrier): Future[Option[YesNo]] = {
    fetchProperty(reference) map { maybeProperty =>
      maybeProperty.flatMap(_.startDateBeforeLimit) map {
        case true => Yes
        case false => No
      }
    }
  }

  def savePropertyStartDateBeforeLimit(reference: String, startDateBeforeLimit: YesNo)
                                      (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    val savedValue = startDateBeforeLimit match {
      case Yes => true
      case No => false
    }

    fetchProperty(reference) map {
      case Some(property) => property.copy(startDateBeforeLimit = Some(savedValue), confirmed = false)
      case None => PropertyModel(startDateBeforeLimit = Some(savedValue))
    } flatMap { model =>
      saveProperty(reference, model)
    }
  }

  def fetchForeignPropertyStartDateBeforeLimit(reference: String)(implicit hc: HeaderCarrier): Future[Option[YesNo]] = {
    fetchOverseasProperty(reference) map { maybeOverseasProperty =>
      maybeOverseasProperty.flatMap(_.startDateBeforeLimit) map {
        case true => Yes
        case false => No
      }
    }
  }

  def saveForeignPropertyStartDateBeforeLimit(reference: String, startDateBeforeLimit: YesNo)
                                             (implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    val savedValue = startDateBeforeLimit match {
      case Yes => true
      case No => false
    }

    fetchOverseasProperty(reference) map {
      case Some(overseasProperty) => overseasProperty.copy(startDateBeforeLimit = Some(savedValue), confirmed = false)
      case None => OverseasPropertyModel(startDateBeforeLimit = Some(savedValue))
    } flatMap { model =>
      saveOverseasProperty(reference, model)
    }
  }

  def fetchOverseasProperty(reference: String)(implicit hc: HeaderCarrier): Future[Option[OverseasPropertyModel]] =
    incomeTaxSubscriptionConnector.getSubscriptionDetails[OverseasPropertyModel](reference, OverseasProperty)

  def fetchIncomeSourcesConfirmation(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](reference, SubscriptionDataKeys.IncomeSourceConfirmation)
  }

  def saveIncomeSourcesConfirmation(reference: String)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[Boolean](reference, SubscriptionDataKeys.IncomeSourceConfirmation, true)
  }

  def saveOverseasProperty(reference: String, overseasProperty: OverseasPropertyModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] =
    incomeTaxSubscriptionConnector.saveSubscriptionDetails[OverseasPropertyModel](reference, SubscriptionDataKeys.OverseasProperty, overseasProperty).flatMap {
      result =>
        taskListStatusUpdate(reference, incomeTaxSubscriptionConnector, result)
    }

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

  def fetchForeignPropertyStartDate(reference: String)(implicit hc: HeaderCarrier): Future[Option[DateModel]] =
    fetchOverseasProperty(reference).map(_.flatMap(_.startDate))

  def saveForeignPropertyStartDate(reference: String, propertyStartDate: DateModel)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
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

  def fetchAllSelfEmployments(reference: String)(implicit hc: HeaderCarrier): Future[(Seq[SelfEmploymentData], Option[AccountingMethod])] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[SoleTraderBusinesses](
      reference = reference,
      id = SoleTraderBusinessesKey
    )(implicitly, SoleTraderBusinesses.encryptedFormat) map {
      case Some(value) => (value.businesses.map(_.toSelfEmploymentData), value.accountingMethod)
      case None => (Seq.empty[SelfEmploymentData], None)
    }
  }

  def fetchAllIncomeSources(reference: String)(implicit hc: HeaderCarrier): Future[IncomeSources] = {
    for {
      (selfEmployments, accountingMethod) <- fetchAllSelfEmployments(reference)
      ukProperty <- fetchProperty(reference)
      foreignProperty <- fetchOverseasProperty(reference)
    } yield {
      IncomeSources(
        selfEmployments,
        accountingMethod,
        ukProperty,
        foreignProperty
      )
    }
  }

  def fetchEligibilityInterruptPassed(reference: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Boolean](
      reference = reference,
      id = EligibilityInterruptPassed
    )
  }

  def saveEligibilityInterruptPassed(reference: String)(implicit hc: HeaderCarrier): Future[PostSubscriptionDetailsResponse] = {
    incomeTaxSubscriptionConnector.saveSubscriptionDetails(
      reference = reference,
      id = EligibilityInterruptPassed,
      data = true
    )
  }

}