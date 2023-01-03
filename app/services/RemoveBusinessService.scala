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
import models.common.business.SelfEmploymentData
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessService @Inject()(val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                     (implicit val ec: ExecutionContext) {


  def deleteBusiness(reference: String, businessId: String, businesses: Seq[SelfEmploymentData])(
    implicit hc: HeaderCarrier
  ): Future[Either[_, _]] = {
    val remainingBusinesses = businesses.filterNot(_.id == businessId)
    incomeTaxSubscriptionConnector
      .saveSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey, remainingBusinesses)
      .flatMap {
        case Right(_) if remainingBusinesses.isEmpty => incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, BusinessAccountingMethod)
        case saveResult@Right(_) => Future.successful(saveResult)
        case fail@Left(_) => Future.successful(fail)
      }
  }

}
