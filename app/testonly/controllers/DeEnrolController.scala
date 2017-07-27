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

//$COVERAGE-OFF$Disabling scoverage on this test only controller as it is only required by our acceptance test

package testonly.controllers

import javax.inject.{Inject, Singleton}

import config.BaseControllerConfig
import connectors.GGAuthenticationConnector
import connectors.models.authenticator.RefreshProfileSuccess
import controllers.AuthenticatedController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService
import testonly.connectors.DeEnrolmentConnector

@Singleton
class DeEnrolController @Inject()(val baseConfig: BaseControllerConfig,
                                  val messagesApi: MessagesApi,
                                  val authService: AuthService,
                                  deEnrolmentConnector: DeEnrolmentConnector,
                                  ggAuthenticationConnector: GGAuthenticationConnector
                                 ) extends AuthenticatedController {

  val resetUsers = Action.async { implicit request =>
    for {
      ggStubResponse <- deEnrolmentConnector.resetUsers()
      authRefreshed <- ggAuthenticationConnector.refreshProfile()
    } yield (authRefreshed, ggStubResponse.status) match {
      case (Right(RefreshProfileSuccess), OK) => Ok("Successfully Reset GG stubbed user")
      case _ => BadRequest(s"Failed to Reset GG stubbed user: ggStubResponse=${ggStubResponse.status}, authRefreshed=$authRefreshed")
    }
  }

  def deEnrol: Action[AnyContent] = Action.async { implicit request =>
    for {
      ggStubResponse <- deEnrolmentConnector.deEnrol()
      authRefreshed <- ggAuthenticationConnector.refreshProfile()
    } yield (ggStubResponse.status, authRefreshed) match {
      case (OK, Right(RefreshProfileSuccess)) => Ok("Successfully De-enrolled")
      case (status, Right(RefreshProfileSuccess)) => BadRequest(s"Failed to De-enrol: status=$status, body=${ggStubResponse.body}")
      case _ => InternalServerError("refresh profile failed")
    }
  }

}

// $COVERAGE-ON$
