/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.subscription.connectors

import javax.inject.Inject

import core.config.AppConfig
import incometax.subscription.httpparsers.RetrieveSubscriptionResponseHttpParser._
import incometax.subscription.httpparsers.StoreSubscriptionResponseHttpParser._
import incometax.subscription.models.StoredSubscription
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionStoreConnector @Inject()(appConfig: AppConfig,
                                           httpClient: HttpClient)(implicit ec: ExecutionContext) {
  def storeSubscriptionData(nino: String, subscriptionData: StoredSubscription)(implicit hc: HeaderCarrier): Future[StoreSubscriptionResponse] = {
    httpClient.POST[StoredSubscription, StoreSubscriptionResponse](appConfig.storeSubscriptionUrl(nino), subscriptionData)
  }

  def retrieveSubscriptionData(nino: String)(implicit hc: HeaderCarrier): Future[RetrieveSubscriptionResponse] = {
    httpClient.GET[RetrieveSubscriptionResponse](appConfig.storeSubscriptionUrl(nino))
  }
}
