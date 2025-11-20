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
import models.No.NO
import models.SessionData
import models.Yes.YES
import models.individual.JourneyStep
import models.individual.JourneyStep.{Confirmation, PreSignUp, SignUp}
import models.requests.individual.{ConfirmationRequest, IdentifierRequest}
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.JsString
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import services.mocks.{MockMandationStatusService, MockReferenceRetrieval, MockSessionDataService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmationJourneyRefinerSpec extends PlaySpec with MockReferenceRetrieval with MockSessionDataService with MockMandationStatusService {

  "ConfirmationJourneyRefiner" when {
    "the user is in a Confirmation state" must {
      "execute the provided code" when {
        "no software status is returned" in {
          mockReference()
          mockGetMandationService(Voluntary, Voluntary)

          val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(Confirmation)), { (request: ConfirmationRequest[_]) =>
              request.utr mustBe utr
              request.nino mustBe nino
              request.mandationStatus mustBe mandationStatus
              request.usingSoftware mustBe false
              request.reference mustBe testReference

              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
        "a no software status is returned" in {
          val sessionData = SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(NO)
          ))
          mockGetMandationService(Voluntary, Voluntary)

          val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(Confirmation), sessionData = sessionData), { (request: ConfirmationRequest[_]) =>
              request.utr mustBe utr
              request.nino mustBe nino
              request.mandationStatus mustBe mandationStatus
              request.usingSoftware mustBe false
              request.reference mustBe testReference

              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
        "a has software status is returned" in {
          val sessionData = SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
          ))
          mockGetMandationService(Voluntary, Voluntary)

          val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(Confirmation), sessionData = sessionData), { (request: ConfirmationRequest[_]) =>
              request.utr mustBe utr
              request.nino mustBe nino
              request.mandationStatus mustBe mandationStatus
              request.usingSoftware mustBe true
              request.reference mustBe testReference

              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
      }
      "throw an internal server exception" when {
        "utr was not present from the identifier request" in {
          mockGetMandationService(Voluntary, Voluntary)
          
          val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(Confirmation), maybeUtr = None), { (_: ConfirmationRequest[_]) =>
              Future.successful(Results.Ok)
            }
          )

          intercept[InternalServerException](await(result))
            .message mustBe "[Individual][ConfirmationJourneyRefiner] - User without utr available in confirmation state"
        }
      }
    }
    "the user is in a PreSignUp state" must {
      "return not found" in {
        val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(PreSignUp)), { (_: ConfirmationRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe NOT_FOUND
      }
    }
    "the user is in a SignUp state" must {
      "return not found" in {
        val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(SignUp)), { (_: ConfirmationRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe NOT_FOUND
      }
    }
    "the user has no state" must {
      "return not found" in {
        val result: Future[Result] = confirmationJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = None), { (_: ConfirmationRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe NOT_FOUND
      }
    }
  }

  lazy val confirmationJourneyRefiner: ConfirmationJourneyRefiner = new ConfirmationJourneyRefiner(
    mockReferenceRetrieval,
    mockMandationStatusService
  )

  lazy val nino: String = "AA000000A"
  lazy val utr: String = "1234567890"
  lazy val mandationStatus: MandationStatusModel = MandationStatusModel(Voluntary, Voluntary)

  def requestWithSession(maybeJourneyStep: Option[JourneyStep]): FakeRequest[_] = {
    maybeJourneyStep match {
      case Some(journeyStep) => FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> journeyStep.key)
      case None => FakeRequest()
    }
  }

  def identifierRequest(journeyStep: Option[JourneyStep] = None, maybeUtr: Option[String] = Some(utr), sessionData: SessionData = SessionData()): IdentifierRequest[_] = {
    IdentifierRequest(
      request = requestWithSession(journeyStep),
      mtditid = None,
      nino = nino,
      utr = maybeUtr,
      sessionData = sessionData
    )
  }
}
