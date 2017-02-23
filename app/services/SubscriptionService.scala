/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.models.subscription.{FERequest, FEResponse, IncomeSourceType}
import connectors.subscription.ProtectedMicroserviceConnector
import models.{DateModel, SummaryModel}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(protectedMicroserviceConnector: ProtectedMicroserviceConnector) {

  type OS = Option[String]

  def submitSubscription(nino: String, summaryData: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[FEResponse]] = {
    val request = FERequest(
      nino = nino,
      incomeSource = IncomeSourceType(summaryData.incomeSource.get.source),
      accountingPeriodStart = summaryData.accountingPeriod.fold[Option[DateModel]](None)(_.startDate),
      accountingPeriodEnd = summaryData.accountingPeriod.fold[Option[DateModel]](None)(_.endDate),
      cashOrAccruals = summaryData.incomeType.fold[OS](None)(_.incomeType),
      tradingName = summaryData.businessName.fold[OS](None)(_.businessName)
    )
    protectedMicroserviceConnector.subscribe(request)
  }

}
