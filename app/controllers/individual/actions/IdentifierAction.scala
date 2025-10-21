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

import common.Constants
import common.Constants.ITSASessionKeys
import config.AppConfig
import models.SessionData.Data
import models.requests.individual.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logging}
import services.SessionDataService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class IdentifierAction @Inject()(val authConnector: AuthConnector,
                                 val parser: BodyParsers.Default,
                                 val config: Configuration,
                                 val env: Environment)
                                (appConfig: AppConfig,
                                 sessionDataService: SessionDataService)
                                (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]
    with AuthorisedFunctions
    with AuthRedirects
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino) {
      case Some(Individual | Organisation) ~ allEnrolments ~ Some(User) ~ confidenceLevel ~ maybeNino if confidenceLevel.level >= ConfidenceLevel.L250.level =>
        val maybeMTDITID: Option[String] = allEnrolments.getEnrolment(Constants.mtdItsaEnrolmentName).flatMap(_.identifiers.headOption.map(_.value))
        val nino: String = maybeNino.getOrElse(throw new InternalServerException("[Individual][IdentifierAction] - CL250 User, no nino in retrieval"))

        sessionDataService.getAllSessionData().flatMap { sessionData =>
          fetchUTRFromEnrolmentsOrSession(allEnrolments, sessionData) flatMap { maybeUTR =>
            block(IdentifierRequest(
              request = request,
              mtditid = maybeMTDITID,
              nino = nino,
              utr = maybeUTR,
              sessionData = sessionData
            ))
          }
        }
      case Some(Individual | Organisation) ~ _ ~ Some(User) ~ _ ~ _ =>
        Future.successful(Redirect(appConfig.identityVerificationURL).addingToSession(ITSASessionKeys.IdentityVerificationFlag -> "true")(request))
      case Some(Individual | Organisation) ~ _ ~ _ ~ _ ~ _ =>
        logger.info(s"[Individual][IdentifierAction] - Non 'User' credential role. Redirecting to cannot use service page.")
        Future.successful(Redirect(controllers.individual.matching.routes.CannotUseServiceController.show()))
      case _ =>
        logger.info(s"[Individual][IdentifierAction] - User with non individual or organisation affinity. Redirecting to affinity group error page.")
        Future.successful(Redirect(controllers.individual.matching.routes.AffinityGroupErrorController.show))
    } recover {
      case _: AuthorisationException =>
        logger.info(s"[Individual][IdentifierAction] - Authorisation exception from auth caught. Redirecting user to login.")
        toGGLogin(request.path)
    }
  }

  private def fetchUTRFromEnrolmentsOrSession(allEnrolments: Enrolments, sessionData: Data): Future[Option[String]] = {
    allEnrolments.getEnrolment(Constants.utrEnrolmentName).flatMap(_.identifiers.headOption.map(_.value)) match {
      case Some(value) => Future.successful(Some(value))
      case None => Future.successful(sessionDataService.fetchUTR(sessionData))
    }
  }

}
