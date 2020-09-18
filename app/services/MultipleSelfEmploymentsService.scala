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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccess
import javax.inject.{Inject, Singleton}
import models.common.BusinessNameModel
import models.individual.business.{BusinessAddressModel, BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import services.MultipleSelfEmploymentsService.SaveSelfEmploymentDataFailure
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataKeys.BusinessesKey

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MultipleSelfEmploymentsService @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                              (implicit ec: ExecutionContext) {

  def fetchBusinessStartDate(businessId: String)(implicit hc: HeaderCarrier): Future[Option[BusinessStartDate]] = {
    findData[BusinessStartDate](businessId, _.businessStartDate)
  }

  def saveBusinessStartDate(businessId: String, businessStartDate: BusinessStartDate)
                           (implicit hc: HeaderCarrier): Future[Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]] = {
    saveData(businessId, _.copy(businessStartDate = Some(businessStartDate)))
  }

  def fetchBusinessName(businessId: String)(implicit hc: HeaderCarrier): Future[Option[BusinessNameModel]] = {
    findData[BusinessNameModel](businessId, _.businessName)
  }

  def saveBusinessName(businessId: String, businessName: BusinessNameModel)
                      (implicit hc: HeaderCarrier): Future[Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]] = {
    saveData(businessId, _.copy(businessName = Some(businessName)))
  }

  def fetchBusinessTrade(businessId: String)(implicit hc: HeaderCarrier): Future[Option[BusinessTradeNameModel]] = {
    findData[BusinessTradeNameModel](businessId, _.businessTradeName)
  }

  def saveBusinessTrade(businessId: String, businessTrade: BusinessTradeNameModel)
                       (implicit hc: HeaderCarrier): Future[Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]] = {
    saveData(businessId, _.copy(businessTradeName = Some(businessTrade)))
  }

  def fetchBusinessAddress(businessId: String)(implicit hc: HeaderCarrier): Future[Option[BusinessAddressModel]] = {
    findData[BusinessAddressModel](businessId, _.businessAddress)
  }

  def saveBusinessAddress(businessId: String, businessAddress: BusinessAddressModel)
                         (implicit hc: HeaderCarrier): Future[Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]] = {
    saveData(businessId, _.copy(businessAddress = Some(businessAddress)))
  }

  def fetchAllBusinesses(implicit hc: HeaderCarrier): Future[Seq[SelfEmploymentData]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey) map {
      case Some(data) => data
      case _ => Seq.empty[SelfEmploymentData]
    }
  }

  private[services] def findData[T](businessId: String, modelToData: SelfEmploymentData => Option[T])(implicit hc: HeaderCarrier) = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey) map {
      case Some(businesses) => businesses.find(_.id == businessId).flatMap(modelToData)
      case _ => None
    }
  }

  private[services] def saveData(businessId: String, businessUpdate: SelfEmploymentData => SelfEmploymentData)
                                (implicit hc: HeaderCarrier) = {

    def updateBusinessList(businesses: Seq[SelfEmploymentData]): Seq[SelfEmploymentData] = {
      if (businesses.exists(_.id == businessId)) {
        businesses map {
          case business if business.id == businessId => businessUpdate(business)
          case business => business
        }
      } else {
        businesses :+ businessUpdate(SelfEmploymentData(businessId))
      }
    }

    incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey) flatMap {
      case data =>
        val updatedBusinessList: Seq[SelfEmploymentData] = updateBusinessList(data.toSeq.flatten)
        incomeTaxSubscriptionConnector.saveSubscriptionDetails(BusinessesKey, updatedBusinessList) map {
          case Right(result) => Right(result)
          case Left(_) => Left(SaveSelfEmploymentDataFailure)
        }
      case _ => Future.successful(Left(SaveSelfEmploymentDataFailure))
    }

  }

}

object MultipleSelfEmploymentsService {

  case object SaveSelfEmploymentDataFailure

}
