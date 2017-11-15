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

package agent.services

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.connectors.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.connectors.UserLockoutConnector

import scala.concurrent.Future

@Singleton
class AgentLockoutService @Inject()(agentLockoutConnector: UserLockoutConnector,
                                    logging: Logging) {

  def lockoutAgent(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] = {
    logging.debug(s"Creating a lock for agent with arn=$arn")
    agentLockoutConnector.lockoutUser(arn)
  }

  def getLockoutStatus(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] = {
    logging.debug(s"Getting lockout status for arn=$arn")
    agentLockoutConnector.getLockoutStatus(arn)
  }

}
