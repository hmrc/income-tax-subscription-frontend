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

import config.AppConfig
import connectors.matching.AuthenticatorConnector
import models.agent.ClientDetailsModel

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class ClientMatchingService @Inject()(val appConfig: AppConfig,
                                      authenticatorConnector: AuthenticatorConnector) {

  /* N.B. this is header update is to be used in conjunction with the test only route
  *  MatchingStubController
  *  the True-Client-IP must match the testId in in testonly.connectors.Request sent
  *  The hc must not be edited in production
  */
  def amendHCForTest(implicit hc: HeaderCarrier): HeaderCarrier =
    appConfig.hasEnabledTestOnlyRoutes match {
      case true => hc.copy(trueClientIp =  Some("ITSA-AGENT"))
      case false => hc
    }

  @inline def matchClient(clientDetailsModel: ClientDetailsModel)(implicit hc: HeaderCarrier): Future[Option[String]] =
    authenticatorConnector.matchClient(clientDetailsModel)(amendHCForTest)

}
