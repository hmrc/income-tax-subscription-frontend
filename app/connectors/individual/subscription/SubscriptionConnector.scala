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

package connectors.individual.subscription

import config.AppConfig
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser._
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser._
import javax.inject.{Inject, Singleton}
import models.individual.subscription.SubscriptionRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConnector @Inject()(val appConfig: AppConfig,
                                      val http: HttpClient)
                                     (implicit ec: ExecutionContext) {

  def subscriptionUrl(nino: String): String = appConfig.subscriptionUrl + SubscriptionConnector.subscriptionUri(nino)

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    http.GET[GetSubscriptionResponse](subscriptionUrl(nino))
  }

  def subscriptionUrlPost(nino: String): String = s"${appConfig.subscriptionUrlPost}/$nino"

  def subscribe(request: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[SubscriptionResponse] =
    http.POST[SubscriptionRequest, SubscriptionResponse](subscriptionUrlPost(request.nino), request)

}

object SubscriptionConnector {

  def subscriptionUri(nino: String): String = "/" + nino

}
