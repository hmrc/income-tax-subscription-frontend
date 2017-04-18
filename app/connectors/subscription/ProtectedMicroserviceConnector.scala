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
import connectors.models.subscription.{FERequest, FEResponse, FESuccessResponse}
import play.api.http.Status.OK
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProtectedMicroserviceConnector @Inject()(val appConfig: AppConfig,
                                               val httpPost: HttpPost,
                                               val httpGet: HttpGet) {

  lazy val subscriptionUrl: String => String = nino => s"${appConfig.subscriptionUrl}/$nino"

  def subscribe(request: FERequest)(implicit hc: HeaderCarrier): Future[Option[FEResponse]] = {
    httpPost.POST[FERequest, HttpResponse](subscriptionUrl(request.nino), request).map {
      response =>
        response.status match {
          case OK => response.json.as[FESuccessResponse]
          case _ => None
        }
    }
  }

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[Option[FEResponse]] = {
    httpGet.GET[HttpResponse](subscriptionUrl(nino)).map {
      response =>
        response.status match {
          case OK => response.json.as[FESuccessResponse]
          case _ => None
        }
    }
  }

}
