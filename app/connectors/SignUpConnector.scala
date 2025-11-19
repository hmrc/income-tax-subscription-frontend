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
import connectors.httpparser.SignUpResponseHttpParser._
import models.AccountingYear
import models.common.subscription.SignUpRequestModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpConnector @Inject()(appConfig: AppConfig, http: HttpClientV2)
                               (implicit ec: ExecutionContext) {

  def signUp(nino: String, utr: String, taxYear: AccountingYear)
            (implicit hc: HeaderCarrier): Future[SignUpResponse] =
    http.post(url"${appConfig.signUpUrl}")
      .withBody(Json.toJson(SignUpRequestModel(
        nino = nino,
        utr = utr,
        taxYear = taxYear)))
      .execute[SignUpResponse]
}