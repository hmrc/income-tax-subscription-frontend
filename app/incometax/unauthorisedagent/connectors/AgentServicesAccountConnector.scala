/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.unauthorisedagent.connectors

import javax.inject.Inject

import core.config.AppConfig
import incometax.unauthorisedagent.httpparsers.GetAgencyNameResponseHttpParser._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AgentServicesAccountConnector @Inject()(appConfig: AppConfig,
                                              httpClient: HttpClient)(implicit ec: ExecutionContext) {

  def getAgencyName(arn: String)(implicit hc: HeaderCarrier): Future[GetAgencyNameResponse] =
    httpClient.GET[GetAgencyNameResponse](appConfig.getAgencyNameUrl(arn))

}
