/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.errors

import config.AppConfig
import controllers.SignUpBaseController
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import services.AuthService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

abstract class ErrorBaseController(authService: AuthService,
                                   appConfig: AppConfig)
                                  (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext)
extends SignUpBaseController {
  protected final def authenticate[A](request: Request[A])(f: Boolean => Result)(implicit hc: HeaderCarrier): Future[Result] = {
    authService.authorised().retrieve(affinityGroup) {
      case Some(Agent) => Future.successful(f(true)) // Agent user
      case _ => Future.successful(f(false)) // Individual or Organisation user
    } recover {
      case _: AuthorisationException =>
        logger.error(s"[ContactHMRCController] - Authorisation exception from auth caught. Redirecting user to login.")
        appConfig.redirectToLogin(request.path)
    }
  }
}
