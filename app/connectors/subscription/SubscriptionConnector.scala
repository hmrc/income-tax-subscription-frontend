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

import config.AppConfig
import connectors.models.subscription.{SubscriptionFailureResponse, SubscriptionRequest, SubscriptionSuccessResponse}
import play.api.http.Status.OK
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionConnector @Inject()(val appConfig: AppConfig,
                                      val httpPost: HttpPost,
                                      val httpGet: HttpGet) {
  import SubscriptionConnector._

  def subscriptionUrl(nino: String): String = appConfig.subscriptionUrl + SubscriptionConnector.subscriptionUri(nino)

  def subscribe(request: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[Either[SubscriptionFailureResponse, SubscriptionSuccessResponse]] = {
    httpPost.POST[SubscriptionRequest, HttpResponse](subscriptionUrl(request.nino), request).map {
      response =>
        response.status match {
          case OK =>
            response.json.asOpt[SubscriptionSuccessResponse] match {
              case Some(successResponse) => Right(successResponse)
              case _ => Left(SubscriptionFailureResponse(badlyFormattedMessage))
            }
          case status => Left(SubscriptionFailureResponse(subscriptionErrorText(status)))
        }
    }
  }

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[Either[SubscriptionFailureResponse, Option[SubscriptionSuccessResponse]]] = {
    httpGet.GET[HttpResponse](subscriptionUrl(nino)).map {
      response =>
        response.status match {
          case OK => Right(response.json.asOpt[SubscriptionSuccessResponse])
          case status => Left(SubscriptionFailureResponse(subscriptionErrorText(status)))
        }
    }
  }

}

object SubscriptionConnector {

  def subscriptionUri(nino: String): String = "/" + nino

  def subscriptionErrorText(status: Int) = s"Back end service returned $status"

  val badlyFormattedMessage = "Badly formatted subscription response"
}