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
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser.*
import models.common.subscription.CreateIncomeSourcesModel
import org.apache.pekko.actor.ActorSystem
import play.api.http.Status.{BAD_GATEWAY, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utilities.UUIDProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class CreateIncomeSourcesConnector @Inject()(http: HttpClientV2,
                                             uuidProvider: UUIDProvider,
                                             appConfig: AppConfig,
                                             override protected val actorSystem: ActorSystem,
                                             override protected val configuration: Config)
                                            (implicit ec: ExecutionContext) extends ConnectorRetries {

  private val retryWithSameIdempotencyStatuses: Set[Int] = Set(BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
  private val retryWithNewIdempotencyCodes: Set[String] = Set("003")
  private val retryWithSameIdempotencyCodes: Set[String] = Set("830")

  def createIncomeSources(mtdbsa: String, request: CreateIncomeSourcesModel)
                         (implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] =
    retryWithIdempotency[CreateIncomeSourcesResponse]("Create Income Sources", uuidProvider.getUUID) {
      case (Left(UnexpectedStatus(status, _)), currentIdempotencyKey) if retryWithSameIdempotencyStatuses.contains(status) =>
        currentIdempotencyKey
      case (Left(UnexpectedStatus(UNPROCESSABLE_ENTITY, Some(code))), currentIdempotencyKey) if retryWithSameIdempotencyCodes.contains(code) =>
        currentIdempotencyKey
      case (Left(UnexpectedStatus(UNPROCESSABLE_ENTITY, Some(code))), _) if retryWithNewIdempotencyCodes.contains(code) =>
        uuidProvider.getUUID
    } { key =>
      updateBackend(
        mtdbsa = mtdbsa,
        request = request.copy(
          idempotencyKey = Some(key)
        )
      )
    }.andThen(logFailure)

  private def updateBackend(mtdbsa: String, request: CreateIncomeSourcesModel)
                           (implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] =
    http
      .post(url"${s"${appConfig.createIncomeSourcesUrl}/$mtdbsa"}")
      .withBody(Json.toJson(request))
      .execute[CreateIncomeSourcesResponse]

  private def logFailure: PartialFunction[Try[CreateIncomeSourcesResponse], Unit] = {
    case Success(Left(error)) =>
      val message = s"[Create Income Sources] Unexpected response: status = ${error.status}"
      logger.error(error.code.fold(message)(code => s"$message, code = $code"))
  }
}
