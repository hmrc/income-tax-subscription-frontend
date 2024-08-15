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
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.{DeleteSubscriptionDetailsSuccess, DeleteSubscriptionDetailsSuccessResponse}
import models.AccountingMethod
import models.common.business.SelfEmploymentData
import services.RemoveBusinessService.{DeleteBusinessesFailure, RemoveBusinessFailure, SaveBusinessFailure}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessService @Inject()(val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                      val subscriptionDetailsService: SubscriptionDetailsService)
                                     (implicit val ec: ExecutionContext) {


  def deleteBusiness(reference: String, businessId: String, businesses: Seq[SelfEmploymentData], accountingMethod: Option[AccountingMethod])(
    implicit hc: HeaderCarrier
  ): Future[Either[RemoveBusinessFailure, DeleteSubscriptionDetailsSuccess]] = {
    val remainingBusinesses = businesses.filterNot(_.id == businessId)

    subscriptionDetailsService.saveBusinesses(reference, remainingBusinesses, accountingMethod)
      .flatMap {
        case Right(_) => if (remainingBusinesses.isEmpty) {
          incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SoleTraderBusinessesKey) map {
            case Right(value) => Right(value)
            case Left(_) => Left(DeleteBusinessesFailure)
          }
        } else {
          Future.successful(Right(DeleteSubscriptionDetailsSuccessResponse))
        }
        case Left(_) => Future.successful(Left(SaveBusinessFailure))
      }
  }

}

object RemoveBusinessService {

  sealed trait RemoveBusinessFailure

  case object SaveBusinessFailure extends RemoveBusinessFailure

  case object DeleteBusinessesFailure extends RemoveBusinessFailure

}