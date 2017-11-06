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

package agent.connectors.subscription

import javax.inject.{Inject, Singleton}

import agent.connectors.httpparsers.GetSubscriptionResponseHttpParser._
import agent.connectors.httpparsers.SubscriptionResponseHttpParser._
import agent.connectors.models.subscription.SubscriptionRequest
import core.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class SubscriptionConnector @Inject()(val appConfig: AppConfig,
                                      val http: HttpClient) {

  def subscriptionUrl(nino: String): String = appConfig.subscriptionUrl + SubscriptionConnector.subscriptionUri(nino)

  def subscribe(request: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[SubscriptionResponse] =
    http.POST[SubscriptionRequest, SubscriptionResponse](subscriptionUrl(request.nino), request)

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    http.GET[GetSubscriptionResponse](subscriptionUrl(nino))
}

object SubscriptionConnector {

  def subscriptionUri(nino: String): String = "/" + nino

}