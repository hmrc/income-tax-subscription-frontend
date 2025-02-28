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

package controllers.individual.actions

import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.JourneyStateKey
import models.individual.JourneyStep
import models.individual.JourneyStep._
import models.requests.individual.{IdentifierRequest, PreSignUpRequest}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreSignUpJourneyRefiner @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, PreSignUpRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, PreSignUpRequest[A]]] = {
    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep
        )
      } match {
      case Some(PreSignUp) =>
        request.mtditid match {
          case Some(_) =>
            logger.info("[Individual][PreSignUpJourneyRefiner] - MTDITID present on users cred. Sending to already enrolled page")
            Future.successful(Left(Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show)))
          case None =>
            Future.successful(Right(PreSignUpRequest(request, request.nino, request.utr)))
        }
      case Some(SignUp) =>
        if (request.session.get(ITSASessionKeys.SPSEntityId).isDefined) {
          logger.info(s"[Individual][PreSignUpJourneyRefiner] - SPS entity id found in session. Sending to using software page")
          Future.successful(Left(Redirect(controllers.individual.routes.UsingSoftwareController.show())))
        } else {
          logger.info(s"[Individual][PreSignUpJourneyRefiner] - Sign up state without an sps entity id. Sending to SPS")
          Future.successful(Left(Redirect(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS)))
        }
      case Some(Confirmation) =>
        logger.info(s"[Individual][PreSignUpJourneyRefiner] - User is in a confirmation state. Sending the user to the confirmation page")
        Future.successful(Left(Redirect(controllers.individual.routes.ConfirmationController.show)))
      case None =>
        logger.info(s"[Individual][PreSignUpJourneyRefiner] - User has no state. Adding PreSignUp state and sending home.")
        Future.successful(Left(
          Redirect(controllers.individual.matching.routes.HomeController.index).addingToSession(JourneyStateKey -> PreSignUp.key)(request)
        ))
    }
  }

}
