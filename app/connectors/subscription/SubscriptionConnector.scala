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

package connectors.subscription

import javax.inject.{Inject, Singleton}

import core.config.AppConfig
import connectors.models.subscription.SubscriptionRequest
import connectors.httpparsers.SubscriptionResponseHttpParser._
import connectors.httpparsers.GetSubscriptionResponseHttpParser._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost }
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class SubscriptionConnector @Inject()(val appConfig: AppConfig,
                                      val httpPost: HttpPost,
                                      val httpGet: HttpGet) {

  def subscriptionUrl(nino: String): String = appConfig.subscriptionUrl + SubscriptionConnector.subscriptionUri(nino)

  def subscribe(request: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[SubscriptionResponse] =
    httpPost.POST[SubscriptionRequest, SubscriptionResponse](subscriptionUrl(request.nino), request)

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    httpGet.GET[GetSubscriptionResponse](subscriptionUrl(nino))
}

object SubscriptionConnector {

  def subscriptionUri(nino: String): String = "/" + nino

}