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
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsResponse
import connectors.httpparser.GetSubscriptionDetailsHttpParser._
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import connectors.httpparser.RetrieveReferenceHttpParser._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxSubscriptionConnector @Inject()(appConfig: AppConfig,
                                               http: HttpClient)
                                              (implicit ec: ExecutionContext) {

  private def subscriptionURL(reference: String, id: String): String =
    s"${appConfig.microServiceUrl}/income-tax-subscription/subscription-data/$reference/id/$id"

  private def retrieveReferenceUrl: String = {
    appConfig.microServiceUrl + "/income-tax-subscription/subscription-data"
  }

  private def deleteURL(reference: String): String = {
    appConfig.microServiceUrl + s"/income-tax-subscription/subscription-data/$reference"
  }

  def deleteAll(reference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE[HttpResponse](deleteURL(reference))
  }

  def deleteSubscriptionDetails(reference: String, key: String)
                               (implicit hc: HeaderCarrier): Future[DeleteSubscriptionDetailsResponse] = {
    http.DELETE[DeleteSubscriptionDetailsResponse](subscriptionURL(reference, key))
  }

  def saveSubscriptionDetails[T](reference: String, id: String, data: T)
                                (implicit hc: HeaderCarrier, writes: Writes[T]): Future[PostSubscriptionDetailsResponse] = {
    http.POST[JsValue, PostSubscriptionDetailsResponse](subscriptionURL(reference, id), Json.toJson(data))
  }

  def getSubscriptionDetails[T](reference: String, id: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Option[T]] = {
    http.GET[Option[T]](subscriptionURL(reference, id))
  }

  def getSubscriptionDetailsSeq[T](reference: String, id: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Seq[T]] = {
    getSubscriptionDetails[Seq[T]](reference: String, id: String).map(_.getOrElse(Seq.empty))
  }

  def retrieveReference(utr: String)(implicit hc: HeaderCarrier): Future[RetrieveReferenceResponse] = {
    http.POST[JsObject, RetrieveReferenceResponse](retrieveReferenceUrl, Json.obj("utr" -> utr))
  }

}




