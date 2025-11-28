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
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser._
import models.common.subscription.CreateIncomeSourcesModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateIncomeSourcesConnector @Inject()(appConfig: AppConfig, http: HttpClientV2)
                                            (implicit ec: ExecutionContext) {

  def createIncomeSources(mtdbsa: String, request: CreateIncomeSourcesModel)
                         (implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] =
    http
      .post(url"${s"${appConfig.createIncomeSourcesUrl}/$mtdbsa"}")
      .withBody(Json.toJson(request))
      .execute[CreateIncomeSourcesResponse]

}
