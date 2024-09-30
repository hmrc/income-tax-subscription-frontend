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
import models.requests.agent.{ConfirmedClientRequest, IdentifierRequest}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockClientDetailsRetrieval, MockReferenceRetrieval, MockUTRService}
import utilities.UserMatchingSessionUtil.ClientDetails

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmedClientJourneyRefinerSpec extends PlaySpec
  with MockUTRService
  with MockClientDetailsRetrieval
  with MockReferenceRetrieval {

  val confirmedClientJourneyRefiner: ConfirmedClientJourneyRefiner = new ConfirmedClientJourneyRefiner(
    mockUTRService,
    mockClientDetailsRetrieval,
    mockReferenceRetrieval
  )

  val testARN: String = "test-arn"
  val utr: String = "1234567890"
  val clientDetails: ClientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")

  def identifierRequest(journeyStep: Option[JourneyStep]): IdentifierRequest[_] = {
    journeyStep match {
      case Some(step) => IdentifierRequest(FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> step.key), testARN)
      case None => IdentifierRequest(FakeRequest(), testARN)
    }
  }

  "ConfirmedClientJourneyRefiner" must {
    "return a refined ConfirmedClientRequest" when {
      "the request is in a ConfirmedClient journey state" in {
        mockGetUTR(utr)
        mockGetClientDetails(clientDetails.name, clientDetails.nino)
        mockReference()

        val result: Future[Result] = confirmedClientJourneyRefiner.invokeBlock(
          identifierRequest(Some(JourneyStep.ConfirmedClient)), { confirmedClientRequest: ConfirmedClientRequest[_] =>

            confirmedClientRequest.utr mustBe utr
            confirmedClientRequest.clientDetails mustBe clientDetails
            confirmedClientRequest.reference mustBe testReference
            confirmedClientRequest.arn mustBe testARN

            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe OK
      }
    }
    "redirect to the cannot go back page" when {
      "the request has no state" in {
        val result: Future[Result] = confirmedClientJourneyRefiner.invokeBlock(
          identifierRequest(None), { _: ConfirmedClientRequest[_] =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
      }
      "the request has a client details state" in {
        val result: Future[Result] = confirmedClientJourneyRefiner.invokeBlock(
          identifierRequest(Some(JourneyStep.ClientDetails)), { _: ConfirmedClientRequest[_] =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
      }
      "the request has a sign posted state" in {
        val result: Future[Result] = confirmedClientJourneyRefiner.invokeBlock(
          identifierRequest(Some(JourneyStep.SignPosted)), { _: ConfirmedClientRequest[_] =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
      }
    }

    "redirect to the confirmation page" when {
      "the request has a confirmation state" in {
        val result: Future[Result] = confirmedClientJourneyRefiner.invokeBlock(
          identifierRequest(Some(JourneyStep.Confirmation)), { _: ConfirmedClientRequest[_] =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationController.show.url)
      }
    }

  }

}
