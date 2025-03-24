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

package controllers.agent.actions

import common.Constants.ITSASessionKeys
import models.agent.JourneyStep
import models.agent.JourneyStep.{ClientDetails, Confirmation, ConfirmedClient, SignPosted}
import models.requests.agent.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsJourneyRefiner @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, IdentifierRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep,
          clientDetailsConfirmed = request.session.get(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED).isDefined,
          hasMtditid = request.session.get(ITSASessionKeys.MTDITID).isDefined
        )
      } match {
      case Some(ClientDetails | ConfirmedClient | SignPosted) =>
        Future.successful(Right(request))
      case None =>
        logger.info(s"[Agent][ClientDetailsJourneyRefiner] - Incorrect user state, current: None, sending to add another client")
        Future.successful(Left(Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())))
      case Some(Confirmation) =>
        logger.info(s"[Agent][ClientDetailsJourneyRefiner] - Incorrect user state, current: ${Confirmation.key}, sending to confirmation page")
        Future.successful(Left(Redirect(controllers.agent.routes.ConfirmationController.show)))
    }
  }

}
