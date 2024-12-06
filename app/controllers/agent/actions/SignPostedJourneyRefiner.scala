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
import models.requests.agent.{IdentifierRequest, SignPostedRequest}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.agent.ClientDetailsRetrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignPostedJourneyRefiner @Inject()(clientDetailsRetrieval: ClientDetailsRetrieval)
                                        (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, SignPostedRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, SignPostedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep,
          clientDetailsConfirmed = request.session.get(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED).isDefined,
          hasMtditid = request.session.get(ITSASessionKeys.MTDITID).isDefined
        )
      } match {
      case Some(SignPosted) =>
        clientDetailsRetrieval.getClientDetails(request, hc) map { clientDetails =>
          Right(SignPostedRequest(
            request = request,
            clientDetails = clientDetails
          ))
        }
      case state@(None | Some(ConfirmedClient | ClientDetails)) =>
        logger.info(s"[Agent][SignPostedJourneyRefiner] - Incorrect user state, current: ${state.map(_.key)}, sending to cannot go back page")
        Future.successful(Left(Redirect(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show)))
      case Some(Confirmation) =>
        logger.info(s"[Agent][SignPostedJourneyRefiner] - Incorrect user state, current: ${Confirmation.key}, sending to confirmation page")
        Future.successful(Left(Redirect(controllers.agent.routes.ConfirmationController.show)))
    }
  }

}
