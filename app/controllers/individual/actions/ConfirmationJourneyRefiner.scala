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
import models.requests.individual.{IdentifierRequest, SignUpRequest}
import play.api.Logging
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationJourneyRefiner @Inject()(referenceRetrieval: ReferenceRetrieval)
                                          (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, SignUpRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, SignUpRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.session.get(ITSASessionKeys.JourneyStateKey)
      .map { journeyStep =>
        JourneyStep.fromString(
          key = journeyStep
        )
      } match {
      case Some(Confirmation) =>
        for {
          reference <- referenceRetrieval.getIndividualReference(hc, request)
        } yield {
          Right(SignUpRequest(request, reference))
        }
      case state@(None | Some(PreSignUp | SignUp)) =>
        logger.info(s"[Individual][ConfirmationJourneyRefiner] - Incorrect user state, current: ${state.map(_.key)}, showing a not found page")
        Future.successful(Left(NotFound))
    }
  }

}
