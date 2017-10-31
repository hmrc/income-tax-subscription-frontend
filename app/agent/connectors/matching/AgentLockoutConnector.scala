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

package agent.connectors.matching

import javax.inject.{Inject, Singleton}

import core.config.AppConfig
import agent.connectors.httpparsers.LockoutStatusHttpParser._
import agent.connectors.models.matching.LockOutRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AgentLockoutConnector @Inject()(val appConfig: AppConfig,
                                      val httpGet: HttpGet,
                                      val httpPost: HttpPost
                                     ) {

  def agentLockoutUrl(arn: String): String = appConfig.clientMatchingUrl + AgentLockoutConnector.agentLockoutUri(arn)

  def lockoutAgent(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    httpPost.POST[LockOutRequest, LockoutStatusResponse](agentLockoutUrl(arn), LockOutRequest(appConfig.matchingLockOutSeconds))

  def getLockoutStatus(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    httpGet.GET[LockoutStatusResponse](agentLockoutUrl(arn))

}

object AgentLockoutConnector {
  def agentLockoutUri(arn: String): String = s"/lock/$arn"
}
