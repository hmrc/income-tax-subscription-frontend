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

package incometax.unauthorisedagent.services

import javax.inject.Inject

import incometax.unauthorisedagent.connectors.AgentServicesAccountConnector
import incometax.unauthorisedagent.models.{GetAgencyNameFailure, GetAgencyNameSuccess}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

class AgencyNameService @Inject()(agentServicesAccountConnector: AgentServicesAccountConnector
                                    )(implicit ec: ExecutionContext) {
  def getAgencyName(arn: String)(implicit hc: HeaderCarrier): Future[String] =
    agentServicesAccountConnector.getAgencyName(arn) map {
      case Right(GetAgencyNameSuccess(agencyName)) =>
        agencyName
      case Left(GetAgencyNameFailure(reason)) =>
        throw new InternalServerException(s"Failed to get agency name: $reason")
    }
}
