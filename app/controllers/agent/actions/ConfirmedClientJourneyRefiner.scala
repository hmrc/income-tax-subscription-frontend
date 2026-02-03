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
import controllers.utils.ReferenceRetrieval
import models.agent.JourneyStep
import models.agent.JourneyStep.{ClientDetails, Confirmation, ConfirmedClient, SignPosted}
import models.requests.agent.{ConfirmedClientRequest, IdentifierRequest}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.UTRService
import services.agent.ClientDetailsRetrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedClientJourneyRefiner @Inject()(utrService: UTRService,
                                              clientDetailsRetrieval: ClientDetailsRetrieval,
                                              referenceRetrieval: ReferenceRetrieval)
                                             (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, ConfirmedClientRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ConfirmedClientRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep,
          clientDetailsConfirmed = request.session.get(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED).isDefined,
          hasMtditid = request.session.get(ITSASessionKeys.MTDITID).isDefined
        )
      } match {
        case Some(ConfirmedClient) =>
          val sessionData = request.sessionData
          for {
            clientDetails <- clientDetailsRetrieval.getClientDetails(sessionData)(request, hc)
            utr <- utrService.getUTR(sessionData)
            reference <- referenceRetrieval.getReference(Some(request.arn), sessionData)(hc, request)
          } yield {
            Right(ConfirmedClientRequest(
              request = request,
              clientDetails = clientDetails,
              utr = utr,
              reference = reference,
              sessionData = sessionData
            ))
          }
        case state@(None | Some(ClientDetails | SignPosted)) =>
          logger.info(s"[Agent][ConfirmedClientJourneyRefiner] - Incorrect user state, current: ${state.map(_.key)}, sending to cannot go back page")
          Future.successful(Left(Redirect(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show)))
        case Some(Confirmation) =>
          logger.info(s"[Agent][ConfirmedClientJourneyRefiner] - Incorrect user state, current: ${Confirmation.key}, sending to confirmation page")
          Future.successful(Left(Redirect(controllers.agent.routes.ConfirmationController.show)))
      }
  }

}
