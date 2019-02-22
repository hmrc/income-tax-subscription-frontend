/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.unauthorisedagent.models

import core.models.{AccountingMethod, DateModel}
import incometax.subscription.models.IncomeSourceType
import play.api.libs.json.Json

case class StoredSubscription(arn: String,
                              incomeSource: IncomeSourceType,
                              otherIncome: Boolean,
                              currentPeriodIsPrior: Option[Boolean] = None,
                              accountingPeriodStart: Option[DateModel] = None,
                              accountingPeriodEnd: Option[DateModel] = None,
                              tradingName: Option[String] = None,
                              cashOrAccruals: Option[AccountingMethod] = None)

case object StoreSubscriptionSuccess

case class StoreSubscriptionFailure(reason: String)

case class RetrieveSubscriptionFailure(reason: String)

object StoredSubscription {
  implicit val format = Json.format[StoredSubscription]
}

case object DeleteSubscriptionSuccess

case class DeleteSubscriptionFailure(reason: String)
