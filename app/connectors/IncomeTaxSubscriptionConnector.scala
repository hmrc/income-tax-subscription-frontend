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

package connectors

import config.AppConfig
import connectors.httpparser.GetSubscriptionDetailsHttpParser._
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxSubscriptionConnector @Inject()(appConfig: AppConfig,
                                               http: HttpClient)
                                              (implicit ec: ExecutionContext) {

  def subscriptionURL(id: String): String = {
    appConfig.microServiceUrl + s"/income-tax-subscription/self-employments/id/$id"
  }

  def deleteURL(): String = {
    appConfig.microServiceUrl + "/income-tax-subscription/subscription-data/all"
  }

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE(deleteURL())
  }

  def saveSubscriptionDetails[T](id: String, data: T)(implicit hc: HeaderCarrier, writes: Writes[T]): Future[PostSubscriptionDetailsResponse] = {
    http.POST[JsValue, PostSubscriptionDetailsResponse](subscriptionURL(id), Json.toJson(data))
  }

  def getSubscriptionDetails[T](id: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Option[T]] = {
    http.GET[Option[T]](subscriptionURL(id))
  }

}




