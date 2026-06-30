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
import connectors.httpparser.SignUpResponseHttpParser.*
import models.AccountingYear
import models.common.subscription.SignUpFailureResponse.{UnexpectedStatus, UnprocessableSignUp}
import models.common.subscription.SignUpRequestModel
import org.apache.pekko.actor.ActorSystem
import play.api.http.Status.{BAD_GATEWAY, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utilities.UUIDProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpConnector @Inject()(http: HttpClientV2,
                                uuidProvider: UUIDProvider,
                                override val appConfig: AppConfig,
                                override protected val actorSystem: ActorSystem,
                                override protected val configuration: Config)
                               (implicit ec: ExecutionContext) extends ConnectorRetries with FeatureSwitching {

  private val retryWithSameIdempotencyStatuses: Set[Int] = Set(BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
  private val retryWithNewIdempotencyCodes: Set[String] = Set("003")

  def signUp(nino: String, utr: String, taxYear: AccountingYear)
            (implicit hc: HeaderCarrier): Future[SignUpResponse] = {
    if (isEnabled(UseIdempotency)) {
      retryWithIdempotency[SignUpResponse]("sign-up", uuidProvider.getUUID) {
        case (Left(UnexpectedStatus(status)), currentIdempotencyKey) if retryWithSameIdempotencyStatuses.contains(status) =>
          currentIdempotencyKey
        case (Left(UnprocessableSignUp(code, _)), _) if retryWithNewIdempotencyCodes.contains(code) =>
          uuidProvider.getUUID
      } { (idempotencyKey: String) =>
        executeSignUpRequest(nino, utr, taxYear, Some(idempotencyKey))
      }
    } else {
      executeSignUpRequest(nino, utr, taxYear, None)
    }
  }

  private def signUpUrl = url"${appConfig.signUpUrl}"

  private def executeSignUpRequest(nino: String, utr: String, taxYear: AccountingYear, idempotencyKey: Option[String])
                                  (implicit hc: HeaderCarrier): Future[SignUpResponse] = {
    http.post(signUpUrl).withBody(signUpRequestBody(
      nino = nino,
      utr = utr,
      taxYear = taxYear,
      idempotencyKey = idempotencyKey
    )).execute[SignUpResponse]
  }

  private def signUpRequestBody(nino: String, utr: String, taxYear: AccountingYear, idempotencyKey: Option[String]): JsValue = Json.toJson(SignUpRequestModel(
    nino = nino,
    utr = utr,
    taxYear = taxYear,
    idempotencyKey = idempotencyKey
  ))

}