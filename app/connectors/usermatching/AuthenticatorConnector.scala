/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.usermatching

import config.AppConfig
import connectors.usermatching.httpparsers.MatchUserHttpParser._
import models.usermatching.{UserDetailsModel, UserMatchRequestModel}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatorConnector @Inject()(appConfig: AppConfig,
                                       val http: HttpClient)
                                      (implicit ec: ExecutionContext) extends Logging {

  lazy val matchingEndpoint: String = appConfig.authenticatorUrl + "/authenticator/match"

  def matchUser(userDetails: UserDetailsModel)(implicit hc: HeaderCarrier): Future[MatchUserResponse] = {
    val request: UserMatchRequestModel = UserMatchRequestModel(userDetails)

    http.POST[UserMatchRequestModel, MatchUserResponse](matchingEndpoint, request).map {
      case Right(result) =>
        logger.debug("AuthenticatorConnector.matchUser response received: " + result)
        Right(result)
      case Left(error) =>
        logger.warn(s"AuthenticatorConnector.matchUser unexpected response from authenticator: ${error.errors}")
        Left(error)
    }
  }

}
