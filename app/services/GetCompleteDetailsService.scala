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

import models.common.business.{Address, SelfEmploymentData}
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{DateModel, SessionData}
import play.api.libs.json.Format
import services.GetCompleteDetailsService.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import java.time.LocalDate
import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GetCompleteDetailsService @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                          applicationCrypto: ApplicationCrypto)
                                         (implicit ec: ExecutionContext) {

  implicit val jsonCrypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto

  implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  /*
  * Fetches all information about sign up which we display or submit
  * Returns a failure if any data is missing
  * Returns a complete model if all data is present
  */
  def getCompleteSignUpDetails(reference: String, sessionData: SessionData)
                              (implicit hc: HeaderCarrier): Future[Either[GetCompleteDetailsFailure.type, CompleteDetails]] = {

    for {
      maybeSubscriptionData <- subscriptionDetailsService.getAllSubscriptionData(reference)
    } yield maybeSubscriptionData match {
      case Some(subscriptionData) =>
        createCompleteDetails(
          selfEmployments = subscriptionData.fetchAllSelfEmployments,
          ukPropertyBusiness = subscriptionData.fetchProperty,
          foreignPropertyBusiness = subscriptionData.fetchOverseasProperty,
          selectedTaxYear = subscriptionData.fetchSelectedTaxYear(sessionData)
        )
      case None => Left(GetCompleteDetailsFailure)
    }
  }

  private def createCompleteDetails(selfEmployments: Seq[SelfEmploymentData],
                                    ukPropertyBusiness: Option[PropertyModel],
                                    foreignPropertyBusiness: Option[OverseasPropertyModel],
                                    selectedTaxYear: Option[AccountingYearModel]
                                   ): Either[GetCompleteDetailsFailure.type, CompleteDetails] = {

    if (!allIncomeSourcesConfirmed(selfEmployments, ukPropertyBusiness, foreignPropertyBusiness)) {
      Left(GetCompleteDetailsFailure)
    } else {
      selectedTaxYear match {
        case None => Left(GetCompleteDetailsFailure)
        case Some(taxYear) =>
          Try {
            CompleteDetails(
              incomeSources = IncomeSources(
                soleTraderBusinesses = buildSoleTraderBusinesses(selfEmployments),
                ukProperty = buildUkProperty(ukPropertyBusiness),
                foreignProperty = buildForeignProperty(foreignPropertyBusiness)
              ),
              taxYear = taxYear
            )
          } match {
            case Failure(_) => Left(GetCompleteDetailsFailure)
            case Success(completeDetails) => Right(completeDetails)
          }
      }
    }
  }

  private def allIncomeSourcesConfirmed(selfEmployments: Seq[SelfEmploymentData],
                                        ukPropertyBusiness: Option[PropertyModel],
                                        foreignPropertyBusiness: Option[OverseasPropertyModel]): Boolean = {
    selfEmployments.forall(_.confirmed) && ukPropertyBusiness.forall(_.confirmed) && foreignPropertyBusiness.forall(_.confirmed)
  }

  private def buildSoleTraderBusinesses(selfEmployments: Seq[SelfEmploymentData]): Option[SoleTraderBusinesses] = {
    if (selfEmployments.isEmpty) {
      None
    } else {
      Some(SoleTraderBusinesses(
        businesses = selfEmployments.map(buildSoleTraderBusiness)
      ))
    }
  }

  private def buildSoleTraderBusiness(selfEmploymentData: SelfEmploymentData): SoleTraderBusiness = {
    val selectedStartDateBeforeLimit: Boolean = selfEmploymentData.startDateBeforeLimit.contains(true)
    val startDateIsBeforeLimit: Boolean = selfEmploymentData.businessStartDate.exists(_.startDate.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))

    SoleTraderBusiness(
      id = selfEmploymentData.id,
      name = selfEmploymentData.businessName.get.businessName,
      displayName = selfEmploymentData.businessDisplayName,
      trade = selfEmploymentData.businessTradeName.get.businessTradeName,
      startDate = if (selectedStartDateBeforeLimit || startDateIsBeforeLimit) {
        None
      } else {
        Some(selfEmploymentData.businessStartDate.get.startDate.toLocalDate)
      },
      address = selfEmploymentData.businessAddress.get.address
    )
  }

  private def buildUkProperty(ukPropertyBusiness: Option[PropertyModel]): Option[UKProperty] = {
    ukPropertyBusiness.map { property =>
      UKProperty(startDate = buildStartDate(property.startDateBeforeLimit, property.startDate))
    }
  }

  private def buildForeignProperty(foreignPropertyBusiness: Option[OverseasPropertyModel]): Option[ForeignProperty] = {
    foreignPropertyBusiness.map { property =>
      ForeignProperty(startDate = buildStartDate(property.startDateBeforeLimit, property.startDate))
    }
  }

  private def buildStartDate(startDateBeforeLimit: Option[Boolean], startDate: Option[DateModel]): Option[LocalDate] = {
    (startDateBeforeLimit, startDate) match {
      case (Some(true), _) => None
      case (_, Some(date)) =>
        if (date.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit)) {
          None
        } else {
          Some(date.toLocalDate)
        }
      case (_, _) => throw new NoSuchElementException("Start date missing")
    }
  }
}

object GetCompleteDetailsService {

  case class CompleteDetails(incomeSources: IncomeSources,
                             taxYear: AccountingYearModel)

  case class IncomeSources(soleTraderBusinesses: Option[SoleTraderBusinesses],
                           ukProperty: Option[UKProperty],
                           foreignProperty: Option[ForeignProperty])

  case class SoleTraderBusinesses(businesses: Seq[SoleTraderBusiness])

  case class SoleTraderBusiness(id: String,
                                name: String,
                                displayName: Option[String],
                                trade: String,
                                startDate: Option[LocalDate],
                                address: Address)

  case class UKProperty(startDate: Option[LocalDate])

  case class ForeignProperty(startDate: Option[LocalDate])

  case object GetCompleteDetailsFailure

}
