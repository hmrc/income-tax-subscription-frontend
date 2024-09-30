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

import models.JourneyStep
import models.JourneyStep.{ClientDetails, Confirmation, ConfirmedClient}
import models.requests.IdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedClientJourneyRefiner @Inject()(implicit val executionContext: ExecutionContext) extends ActionRefiner[IdentifierRequest, IdentifierRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    request.session.get("JourneyStep").map(JourneyStep.fromString) match {
      case None => Future.successful(Right(request))
      case Some(ClientDetails) => Future.successful(Left(Redirect(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show)))
      case Some(ConfirmedClient) => Future.successful(Right(request))
      case Some(Confirmation) => Future.successful(Left(Redirect(controllers.agent.matching.routes.ClientAlreadySubscribedController.show)))
    }
  }

}
