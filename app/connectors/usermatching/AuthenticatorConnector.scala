/*
 * Copyright 2020 HM Revenue & Customs
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

import core.audit.Logging
import core.config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import connectors.usermatching.httpparsers.MatchUserHttpParser._
import usermatching.models.{UserDetailsModel, UserMatchRequestModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthenticatorConnector @Inject()(appConfig: AppConfig,
                                       val http: HttpClient,
                                       logging: Logging) {

  lazy val matchingEndpoint: String = appConfig.authenticatorUrl + "/authenticator/match"

  def matchUser(userDetails: UserDetailsModel)(implicit hc: HeaderCarrier): Future[MatchUserResponse] = {
    val request: UserMatchRequestModel = UserMatchRequestModel(userDetails)

    def logFailure(message: String): Unit = {
      logging.warn(s"AuthenticatorConnector.matchUser unexpected response from authenticator: $message")
    }

    http.POST[UserMatchRequestModel, MatchUserResponse](matchingEndpoint, request).map {
      case Right(result) =>
        logging.debug("AuthenticatorConnector.matchUser response received: " + result)
        Right(result)
      case Left(error) =>
        logFailure(error.errors)
        Left(error)
    }
  }

}
