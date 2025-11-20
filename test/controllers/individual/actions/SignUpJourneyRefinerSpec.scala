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
import models.individual.JourneyStep.{Confirmation, PreSignUp, SignUp}
import models.requests.individual.{IdentifierRequest, SignUpRequest}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockReferenceRetrieval, MockSessionDataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignUpJourneyRefinerSpec extends PlaySpec with MockReferenceRetrieval with MockSessionDataService {

  "SignUpJourneyRefiner" when {
    "the user is in a SignUp state" should {
      "redirect to already enrolled controller" when {
        "the user has an MTDITID" in {
          val result: Future[Result] = signUpJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(SignUp), Some("test-mtditid")), { (_: SignUpRequest[_]) =>
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AlreadyEnrolledController.show.url)
        }
      }
      "execute the provided code" when {
        "the user does not have an MTDITID" in {
          val result: Future[Result] = signUpJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(SignUp)), { (_: SignUpRequest[_]) =>
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
      }
    }

    "the user is in a Confirmation state" should {
      "redirect to the confirmation page" in {
        val result: Future[Result] = signUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(Confirmation)), { (_: SignUpRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.ConfirmationController.show.url)
      }
    }

    "the user is in a PreSignUp state" should {
      "redirect to the home controller" in {
        val result: Future[Result] = signUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(PreSignUp)), { (_: SignUpRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.HomeController.index.url)
      }
    }

    "the user does not have a journey state" should {
      "redirect to the home controller" in {
        val result: Future[Result] = signUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = None), { (_: SignUpRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.HomeController.index.url)
      }
    }
  }

  lazy val signUpJourneyRefiner: SignUpJourneyRefiner = new SignUpJourneyRefiner(mockReferenceRetrieval, mockSessionDataService)

  def requestWithSession(maybeJourneyStep: Option[JourneyStep]): FakeRequest[_] = {
    maybeJourneyStep match {
      case Some(journeyStep) => FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> journeyStep.key)
      case None => FakeRequest()
    }
  }

  lazy val nino: String = "AA000000A"
  lazy val utr: String = "1234567890"

  def identifierRequest(journeyStep: Option[JourneyStep] = None, mtditid: Option[String] = None): IdentifierRequest[_] = {
    IdentifierRequest(
      request = requestWithSession(journeyStep),
      mtditid = mtditid,
      nino = nino,
      utr = None
    )
  }
}
