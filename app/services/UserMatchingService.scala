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

package services

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.httpparsers.MatchUserHttpParser.MatchUserResponse
import connectors.matching.AuthenticatorConnector
import connectors.models.matching.{UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import models.matching.UserDetailsModel
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


@Singleton
class UserMatchingService @Inject()(val appConfig: AppConfig,
                                    authenticatorConnector: AuthenticatorConnector) {

  /* N.B. this is header update is to be used in conjunction with the test only route
  *  MatchingStubController
  *  the True-Client-IP must match the testId in in testonly.connectors.Request sent
  *  The hc must not be edited in production
  */
  def amendHCForTest(implicit hc: HeaderCarrier): HeaderCarrier =
    appConfig.hasEnabledTestOnlyRoutes match {
      case true => hc.copy(trueClientIp = Some("ITSA"))
      case false => hc
    }

  @inline def matchUser(userDetailsModel: UserDetailsModel)(implicit hc: HeaderCarrier): Future[MatchUserResponse] = {
    authenticatorConnector.matchUser(userDetailsModel)(amendHCForTest)
  }
}
