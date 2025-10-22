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
import controllers.utils.ReferenceRetrieval
import models.individual.JourneyStep
import models.individual.JourneyStep._
import models.requests.individual.{ConfirmationRequest, IdentifierRequest}
import models.{No, Yes}
import play.api.Logging
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionRefiner, Result}
import services.{MandationStatusService, SessionDataService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationJourneyRefiner @Inject()(referenceRetrieval: ReferenceRetrieval,
                                           sessionDataService: SessionDataService,
                                           mandationStatusService: MandationStatusService)
                                          (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, ConfirmationRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ConfirmationRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep
        )
      } match {
      case Some(Confirmation) =>
        val sessionData = request.sessionData
        for {
          reference <- referenceRetrieval.getIndividualReference(sessionData)(hc, request)
          softwareStatus = sessionData.fetchSoftwareStatus
          mandationStatus <- mandationStatusService.getMandationStatus(sessionData)
        } yield {
          val usingSoftware = softwareStatus match {
            case None => false
            case Some(No) => false
            case Some(Yes) => true
          }

          Right(ConfirmationRequest(
            request = request,
            reference = reference,
            nino = request.nino,
            utr = request.utr.getOrElse(
              throw new InternalServerException("[Individual][ConfirmationJourneyRefiner] - User without utr available in confirmation state")
            ),
            usingSoftware = usingSoftware,
            mandationStatus = mandationStatus
          ))
        }
      case state@(None | Some(PreSignUp | SignUp)) =>
        logger.info(s"[Individual][ConfirmationJourneyRefiner] - Incorrect user state, current: ${state.map(_.key)}, showing a not found page")
        Future.successful(Left(NotFound))
    }
  }
}
