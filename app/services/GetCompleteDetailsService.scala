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

import config.AppConfig
import config.featureswitch.FeatureSwitching
import models.AccountingMethod
import models.common.business.{Address, SelfEmploymentData}
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import play.api.Logging
import services.GetCompleteDetailsService._
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import java.time.LocalDate
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GetCompleteDetailsService @Inject()(subscriptionDetailsService: SubscriptionDetailsService, val appConfig: AppConfig)
                                         (implicit ec: ExecutionContext) extends Logging with FeatureSwitching {

  /*
  * Fetches all information about sign up which we display or submit
  * Returns a failure if any data is missing
  * Returns a complete model if all data is present
  */
  def getCompleteSignUpDetails(reference: String)
                              (implicit hc: HeaderCarrier): Future[Either[GetCompleteDetailsFailure.type, CompleteDetails]] = {

    val fetchAllSelfEmployments = subscriptionDetailsService.fetchAllSelfEmployments(reference)
    val fetchUKProperty = subscriptionDetailsService.fetchProperty(reference)
    val fetchForeignProperty = subscriptionDetailsService.fetchOverseasProperty(reference)
    val fetchSelectedTaxYear = subscriptionDetailsService.fetchSelectedTaxYear(reference)

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

    if (selfEmployments.forall(_.confirmed) && ukPropertyBusiness.forall(_.confirmed && foreignPropertyBusiness.forall(_.confirmed))) {
      Try {
        val soleTraderBusinesses: Option[SoleTraderBusinesses] = {
          selfEmploymentsAccountingMethod map { accountingMethod =>
            SoleTraderBusinesses(
              accountingMethod = accountingMethod,
              businesses = selfEmployments.map { selfEmploymentData =>
                val selectedStartDateBeforeLimit: Boolean = selfEmploymentData.startDateBeforeLimit.contains(true)
                val startDateIsBeforeLimit: Boolean = selfEmploymentData.businessStartDate.exists(_.startDate.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))

                SoleTraderBusiness(
                  id = selfEmploymentData.id,
                  name = selfEmploymentData.businessName.get.businessName,
                  trade = selfEmploymentData.businessTradeName.get.businessTradeName,
                  startDate =  if (selectedStartDateBeforeLimit || startDateIsBeforeLimit) {
                    None
                  } else {
                    Some(selfEmploymentData.businessStartDate.get.startDate.toLocalDate)
                  },
                  address = selfEmploymentData.businessAddress.get.address
                )
              }
            )
          }
        }

        val ukProperty: Option[UKProperty] = ukPropertyBusiness.map { property =>
          val selectedStartDateBeforeLimit: Boolean = property.startDateBeforeLimit.contains(true)
          val startDateIsBeforeLimit: Boolean = property.startDate.exists(_.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))

          UKProperty(
            startDate = if (selectedStartDateBeforeLimit || startDateIsBeforeLimit) {
              None
            } else {
              Some(property.startDate.get.toLocalDate)
            },
            accountingMethod = property.accountingMethod.get
          )
        }

        val foreignProperty: Option[ForeignProperty] = foreignPropertyBusiness.map { property =>
          val selectedStartDateBeforeLimit: Boolean = property.startDateBeforeLimit.contains(true)
          val startDateIsBeforeLimit: Boolean = property.startDate.exists(_.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))

          ForeignProperty(
            startDate = if (selectedStartDateBeforeLimit || startDateIsBeforeLimit) {
              None
            } else {
              Some(property.startDate.get.toLocalDate)
            },
            accountingMethod = property.accountingMethod.get
          )
        }

        CompleteDetails(
          incomeSources = IncomeSources(soleTraderBusinesses, ukProperty, foreignProperty),
          taxYear = selectedTaxYear.get
        )
      } match {
        case Failure(_) =>
          logger.error("[GetCompleteDetailsService][getCompleteSignUpDetails] - Failure creating complete details model")
          Left(GetCompleteDetailsFailure)
        case Success(completeDetails) => Right(completeDetails)
      }
    } else {
      logger.error("[GetCompleteDetailsService][getCompleteSignUpDetails] - All income sources not confirmed, failure creating model")
      Left(GetCompleteDetailsFailure)
    }
  }
}

object GetCompleteDetailsService {

  case class CompleteDetails(
                              incomeSources: IncomeSources,
                              taxYear: AccountingYearModel
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
                                 startDate: Option[LocalDate],
                                 address: Address
                               )

  case class UKProperty(
                         startDate: Option[LocalDate],
                         accountingMethod: AccountingMethod
                       )

  case class ForeignProperty(
                              startDate: Option[LocalDate],
                              accountingMethod: AccountingMethod
                            )

  case object GetCompleteDetailsFailure

}
