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

import com.typesafe.config.Config
import config.AppConfig
import config.featureswitch.FeatureSwitch.UseIdempotency
import config.featureswitch.FeatureSwitching
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser.*
import models.common.subscription.CreateIncomeSourcesModel
import org.apache.pekko.actor.ActorSystem
import play.api.http.Status.{BAD_GATEWAY, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateIncomeSourcesConnector @Inject()(
  val appConfig: AppConfig,
  val configuration: Config,
  val actorSystem: ActorSystem,
  http: HttpClientV2
) (implicit ec: ExecutionContext) extends ConnectorRetries with FeatureSwitching {

  def createIncomeSources(mtdbsa: String, request: CreateIncomeSourcesModel)
                         (implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] =
    if (isEnabled(UseIdempotency)) {
      retryWithIdempotency[CreateIncomeSourcesResponse]("Create Income Sources", idempotencyKey()) {
        case (Left(UnexpectedStatus(UNPROCESSABLE_ENTITY)), _) => idempotencyKey()
        case (Left(UnexpectedStatus(BAD_GATEWAY)), key) => key
        case (Left(UnexpectedStatus(SERVICE_UNAVAILABLE)), key) => key
        case (Left(UnexpectedStatus(GATEWAY_TIMEOUT)), key) => key
      } { key =>
        updateBackend(
          mtdbsa = mtdbsa,
          request = request.copy(
            idempotencyKey = Some(key)
          )
        )
      }
    } else {
      updateBackend(mtdbsa, request)
    }

  private def updateBackend(mtdbsa: String, request: CreateIncomeSourcesModel)
                           (implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] =
    http
      .post(url"${s"${appConfig.createIncomeSourcesUrl}/$mtdbsa"}")
      .withBody(Json.toJson(request))
      .execute[CreateIncomeSourcesResponse]
      
  private def idempotencyKey() =
    UUID.randomUUID().toString
}
