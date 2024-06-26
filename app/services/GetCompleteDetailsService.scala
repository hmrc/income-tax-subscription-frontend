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
import models.{AccountingMethod, AccountingYear}
import play.api.Logging
import services.GetCompleteDetailsService._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GetCompleteDetailsService @Inject()(subscriptionDetailsService: SubscriptionDetailsService)
                                         (implicit ec: ExecutionContext) extends Logging {

  /*
  * Fetches all information about sign up which we display or submit
  * Returns a failure if any data is missing
  * Returns a complete model if all data is present
  */
  def getCompleteSignUpDetails(reference: String, nino: String, utr: String)
                              (implicit hc: HeaderCarrier): Future[Either[GetCompleteDetailsFailure.type, CompleteDetails]] = {

    val fetchAllSelfEmployments = subscriptionDetailsService.fetchAllSelfEmployments(reference)
    val fetchUKProperty = subscriptionDetailsService.fetchProperty(reference)
    val fetchForeignProperty = subscriptionDetailsService.fetchOverseasProperty(reference)
    val fetchSelectedTaxYear = subscriptionDetailsService.fetchSelectedTaxYear(reference, nino, utr)

    for {
      (selfEmployments, accountingMethod) <- fetchAllSelfEmployments
      ukProperty <- fetchUKProperty
      foreignProperty <- fetchForeignProperty
      selectedTaxYear <- fetchSelectedTaxYear
    } yield {
      createCompleteDetails(
        selfEmployments,
        accountingMethod,
        ukProperty,
        foreignProperty,
        selectedTaxYear
      )
    }

  }

  private def createCompleteDetails(selfEmployments: Seq[SelfEmploymentData],
                                    selfEmploymentsAccountingMethod: Option[AccountingMethod],
                                    ukPropertyBusiness: Option[PropertyModel],
                                    foreignPropertyBusiness: Option[OverseasPropertyModel],
                                    selectedTaxYear: Option[AccountingYearModel]
                                   ): Either[GetCompleteDetailsFailure.type, CompleteDetails] = {

    Try {
      val soleTraderBusinesses: Option[SoleTraderBusinesses] = {
        selfEmploymentsAccountingMethod map { accountingMethod =>
          SoleTraderBusinesses(
            accountingMethod = accountingMethod,
            businesses = selfEmployments.map { selfEmploymentData =>
              SoleTraderBusiness(
                id = selfEmploymentData.id,
                name = selfEmploymentData.businessName.get.businessName,
                trade = selfEmploymentData.businessTradeName.get.businessTradeName,
                startDate = selfEmploymentData.businessStartDate.get.startDate.toLocalDate,
                address = selfEmploymentData.businessAddress.get.address
              )
            }
          )
        }
      }

      val ukProperty: Option[UKProperty] = ukPropertyBusiness.map { property =>
        UKProperty(
          startDate = property.startDate.get.toLocalDate,
          accountingMethod = property.accountingMethod.get
        )
      }

      val foreignProperty: Option[ForeignProperty] = foreignPropertyBusiness.map { property =>
        ForeignProperty(
          startDate = property.startDate.get.toLocalDate,
          accountingMethod = property.accountingMethod.get
        )
      }

      CompleteDetails(
        incomeSources = IncomeSources(soleTraderBusinesses, ukProperty, foreignProperty),
        taxYear = selectedTaxYear.get.accountingYear
      )
    } match {
      case Failure(_) =>
        logger.error("[GetCompleteDetailsService][getCompleteSignUpDetails] - Failure creating complete details model")
        Left(GetCompleteDetailsFailure)
      case Success(completeDetails) => Right(completeDetails)
    }
  }

}

object GetCompleteDetailsService {

  case class CompleteDetails(
                              incomeSources: IncomeSources,
                              taxYear: AccountingYear
                            )

  case class IncomeSources(
                            soleTraderBusinesses: Option[SoleTraderBusinesses],
                            ukProperty: Option[UKProperty],
                            foreignProperty: Option[ForeignProperty]
                          )

  case class SoleTraderBusinesses(
                                   accountingMethod: AccountingMethod,
                                   businesses: Seq[SoleTraderBusiness]
                                 )

  case class SoleTraderBusiness(
                                 id: String,
                                 name: String,
                                 trade: String,
                                 startDate: LocalDate,
                                 address: Address
                               )

  case class UKProperty(
                         startDate: LocalDate,
                         accountingMethod: AccountingMethod
                       )

  case class ForeignProperty(
                              startDate: LocalDate,
                              accountingMethod: AccountingMethod
                            )

  case object GetCompleteDetailsFailure

}
