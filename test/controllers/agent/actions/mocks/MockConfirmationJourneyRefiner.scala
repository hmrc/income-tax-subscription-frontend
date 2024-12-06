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

package controllers.agent.actions.mocks

import controllers.agent.actions.ConfirmationJourneyRefiner
import controllers.utils.ReferenceRetrieval
import models.requests.agent.{ConfirmedClientRequest, IdentifierRequest}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import services.UTRService
import services.agent.ClientDetailsRetrieval
import utilities.UserMatchingSessionUtil.ClientDetails

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockConfirmationJourneyRefiner extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val clientName: String = "FirstName LastName"
  val nino: String = "ZZ111111Z"
  val utr: String = "1234567890"
  val reference: String = "test-reference"

  val clientDetails: ClientDetails = ClientDetails(clientName, nino)

  val fakeConfirmationJourneyRefiner: ConfirmationJourneyRefiner = new ConfirmationJourneyRefiner(
    mock[UTRService], mock[ClientDetailsRetrieval], mock[ReferenceRetrieval]
  ) {
    override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ConfirmedClientRequest[A]]] = {
      Future.successful(Right(ConfirmedClientRequest(request, clientDetails, utr, reference)))
    }
  }

}
