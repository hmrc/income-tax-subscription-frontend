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
import models.individual.JourneyStep
import models.individual.JourneyStep.*
import models.requests.individual.{ClaimEnrolmentRequest, IdentifierRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimEnrolmentJourneyRefiner @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, ClaimEnrolmentRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ClaimEnrolmentRequest[A]]] = {
    request.sessionData.fetchJourneyStep(request) match {
      case Some(ClaimEnrolment) =>
        Future.successful(Right(ClaimEnrolmentRequest(
          request = request,
          nino = request.nino,
          mtditid = request.mtditid,
          sessionData = request.sessionData
        )))
      case Some(Confirmation) =>
        Future.successful(Left(Redirect(controllers.individual.routes.ConfirmationController.show)))
      case state@(None | Some(PreSignUp | SignUp)) =>
        Future.successful(Left(Redirect(controllers.individual.matching.routes.HomeController.index)))
    }
  }
}
