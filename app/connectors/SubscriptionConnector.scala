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

package connectors

import config.AppConfig
import connectors.httpparser.GetSubscriptionResponseHttpParser._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConnector @Inject()(val appConfig: AppConfig,
                                      val http: HttpClientV2)
                                     (implicit ec: ExecutionContext) {

  def subscriptionUrl(nino: String): String = appConfig.subscriptionUrl + SubscriptionConnector.subscriptionUri(nino)

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    http.get(url"${subscriptionUrl(nino)}").execute[GetSubscriptionResponse]
  }

}

object SubscriptionConnector {

  def subscriptionUri(nino: String): String = "/" + nino

}
