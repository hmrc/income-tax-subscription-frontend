/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.connectors

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.common.Constants._
import core.config.AppConfig
import core.connectors.RawResponseReads
import play.api.http.Status
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.Future._

@Singleton
class AgentServicesConnector @Inject()(appConfig: AppConfig,
                                       http: HttpClient,
                                       logging: Logging) extends RawResponseReads {
  def isPreExistingRelationship(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = agentClientURL(arn, nino)

    http.GET(url).flatMap {
      case res if res.status == Status.OK => successful(true)
      case res if res.status == Status.NOT_FOUND => successful(false)
      case res => failed(isPreExistingRelationshipFailure(res.status, res.body))
    }
  }

  def agentClientURL(arn: String, nino: String): String =
    appConfig.agentMicroserviceUrl + AgentServicesConnector.agentClientURI(arn, nino)

  def isPreExistingRelationshipFailure(status: Int, body: String): Throwable = failure("isPreExistingRelationship", status, body)

  private def failure(methodCall: String, status: Int, body: String) = {
    val message = s"AgentServicesConnector.$methodCall unexpected response from agent services: status=$status body=$body"

    logging.warn(message)
    new InternalServerException(message)
  }

}

object AgentServicesConnector {

  def agentClientURI(arn: String, nino: String): String =
    s"/agent-client-relationships/agent/$arn/service/IR-SA/client/ni/$nino"

}
