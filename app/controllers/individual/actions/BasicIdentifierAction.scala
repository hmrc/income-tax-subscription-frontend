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

package controllers.individual.actions

import config.AppConfig
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class BasicIdentifierAction @Inject()(val authConnector: AuthConnector,
                                      val parser: BodyParsers.Default)
                                     (appConfig: AppConfig)
                                     (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[Request, AnyContent]
    with ActionFunction[Request, Request]
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised() {
      block(request)
    } recover {
      case _: AuthorisationException =>
        logger.info(s"[Individual][IdentifierAction] - Authorisation exception from auth caught. Redirecting user to login.")
        appConfig.redirectToLogin(controllers.individual.matching.routes.HomeController.index.url)
    }
  }

}
