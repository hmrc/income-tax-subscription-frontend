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

package connectors.agent

import config.AppConfig
import connectors.RawResponseReads
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesConnector @Inject()(appConfig: AppConfig,
                                       http: HttpClient)
                                      (implicit ec: ExecutionContext) extends RawResponseReads with Logging {

  def agentClientURL(arn: String, nino: String): String = {
    appConfig.agentMicroserviceUrl + AgentServicesConnector.agentClientURI(arn, nino)
  }

  def isPreExistingRelationship(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = agentClientURL(arn, nino)

    http.GET[HttpResponse](url).map {
      case res if res.status == Status.OK => true
      case res if res.status == Status.NOT_FOUND => false
      case res => throw new InternalServerException(s"[AgentServicesConnector][isPreExistingRelationship] failure, status: ${res.status} body=${res.body}")
    }
  }

  def agentMTDClientURL(arn: String, nino: String): String = {
    appConfig.agentMicroserviceUrl + AgentServicesConnector.agentMTDClientURI(arn, nino)
  }

  def isMTDPreExistingRelationship(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = agentMTDClientURL(arn, nino)

    http.GET[HttpResponse](url).map {
      case res if res.status == Status.OK => true
      case res if res.status == Status.NOT_FOUND => false
      case res => throw new InternalServerException(s"[AgentServicesConnector][isMTDPreExistingRelationship] failure, status: ${res.status} body=${res.body}")
    }
  }

  def suppAgentClientURL(arn: String, nino: String): String = {
    appConfig.agentMicroserviceUrl + AgentServicesConnector.suppAgentClientURI(arn, nino)
  }

  def isMTDSuppAgentRelationship(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = suppAgentClientURL(arn, nino)

    http.GET[HttpResponse](url).map {
      case res if res.status == Status.OK => true
      case res =>
        logger.warn(s"[AgentServicesConnector][isMTDSuppAgentRelationship] - Unexpected status returned - ${res.status}")
        false
    }
  }
}

object AgentServicesConnector {

  def agentClientURI(arn: String, nino: String): String =
    s"/agent-client-relationships/agent/$arn/service/IR-SA/client/ni/$nino"

  def agentMTDClientURI(arn: String, nino: String): String =
    s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT/client/ni/$nino"

  def suppAgentClientURI(arn: String, nino: String): String =
    s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-SUPP/client/ni/$nino"

}