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

import controllers.individual.resolvers.AlreadyEnrolledResolver
import models.individual.JourneyStep
import models.individual.JourneyStep.*
import models.requests.individual.{IdentifierRequest, PreSignUpRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.SessionDataService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreSignUpJourneyRefiner @Inject(resolver: AlreadyEnrolledResolver,
                                      sessionDataService: SessionDataService)
                                     (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, PreSignUpRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, PreSignUpRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    request.sessionData.fetchJourneyStep(request) match {
      case Some(PreSignUp | ClaimEnrolment) =>
        request.mtditid match {
          case Some(mtditid) =>
            resolver.resolve(nino = request.nino, sessionData = request.sessionData) map { call =>
              Left(Redirect(call))
            }
          case None =>
            Future.successful(Right(PreSignUpRequest(request, request.nino, request.utr)))
        }
      case Some(SignUp) =>
        Future.successful(Right(PreSignUpRequest(request, request.nino, request.utr)))
      case Some(Confirmation) =>
        Future.successful(Left(Redirect(controllers.individual.routes.ConfirmationController.show)))
      case None =>
        sessionDataService.saveJourneyStep(PreSignUp).map { _ => Left(
          Redirect(controllers.individual.matching.routes.HomeController.index)
        )}
    }
  }
}
