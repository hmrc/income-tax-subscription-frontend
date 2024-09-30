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

package controllers.agent.actions

import common.Constants
import models.requests.agent.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class IdentifierAction @Inject()(val authConnector: AuthConnector,
                                 val parser: BodyParsers.Default,
                                 val config: Configuration,
                                 val env: Environment)
                                (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]
    with AuthorisedFunctions
    with AuthRedirects
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(affinityGroup and allEnrolments) {
      case Some(Agent) ~ allEnrolments =>
        allEnrolments.getEnrolment(Constants.hmrcAsAgent).flatMap { enrolment =>
          enrolment.identifiers.headOption.map(_.value)
        } match {
          case Some(arn) =>
            block(IdentifierRequest(request, arn))
          case None =>
            logger.info(s"[Agent][IdentifierAction] - Agent user without agent reference number. Redirecting to not enrolled in agent services.")
            Future.successful(Redirect(controllers.agent.matching.routes.NotEnrolledAgentServicesController.show))
        }
      case _ =>
        logger.info(s"[Agent][IdentifierAction] - User with non agent affinity. Redirecting to not enrolled in agent services.")
        Future.successful(Redirect(controllers.agent.matching.routes.NotEnrolledAgentServicesController.show))
    } recover {
      case _: AuthorisationException =>
        logger.info(s"[Agent][IdentifierAction] - Authorisation exception from auth caught. Redirecting user to login.")
        toGGLogin(request.path)
    }
  }
}
