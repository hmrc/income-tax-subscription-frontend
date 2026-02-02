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
import models.requests.individual.{IdentifierRequest, PreSignUpRequest}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreSignUpJourneyRefinerSpec extends PlaySpec {

  "PreSignUpJourneyRefiner" when {
    "the user is in a PreSignUp state" should {
      "redirect to the already enrolled page" when {
        "the user already has an MTDITID on their cred" in {
          val result: Future[Result] = preSignUpJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(PreSignUp), Some(testEntityId), Some(testUtr), Some(testMTDITID)), { (_: PreSignUpRequest[_]) =>
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AlreadyEnrolledController.show.url)
        }
      }
      "execute the provided code" when {
        "the user does not have an MTDITID" in {
          val result: Future[Result] = preSignUpJourneyRefiner.invokeBlock(
            identifierRequest(journeyStep = Some(PreSignUp), Some(testEntityId), Some(testUtr), None), { (request: PreSignUpRequest[_]) =>
              request.nino mustBe testNino
              request.utr mustBe Some(testUtr)

              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
      }
    }
    "the user is in a SignUp state" should {
      "continue as normal" in {
        val result: Future[Result] = preSignUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(SignUp), None, Some(testUtr), None), { _ =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe OK
      }
    }
    "the user is in a Confirmation state" should {
      "redirect to the confirmation page" in {
        val result: Future[Result] = preSignUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = Some(Confirmation), Some(testEntityId), Some(testUtr), None), { (_: PreSignUpRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.ConfirmationController.show.url)
      }
    }
    "the user has no state" should {
      "redirect to the index route with the PreSignUp state" in {
        val result: Future[Result] = preSignUpJourneyRefiner.invokeBlock(
          identifierRequest(journeyStep = None, Some(testEntityId), Some(testUtr), None), { (_: PreSignUpRequest[_]) =>
            Future.successful(Results.Ok)
          }
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.HomeController.index.url)
      }
    }
  }

  lazy val preSignUpJourneyRefiner: PreSignUpJourneyRefiner = new PreSignUpJourneyRefiner

  lazy val testNino: String = "AA000000A"
  lazy val testUtr: String = "1234567890"
  lazy val testMTDITID: String = "XAIT0000000001"
  lazy val testEntityId: String = "test-entity-id"

  def requestWithSession(maybeJourneyStep: Option[JourneyStep], maybeEntityId: Option[String]): FakeRequest[_] = {
    val requestWithJourneyStep: FakeRequest[_] = maybeJourneyStep match {
      case Some(journeyStep) => FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> journeyStep.key)
      case None => FakeRequest()
    }

    maybeEntityId match {
      case Some(entityId) => requestWithJourneyStep.withSession(ITSASessionKeys.SPSEntityId -> entityId)
      case None => requestWithJourneyStep
    }
  }

  def identifierRequest(journeyStep: Option[JourneyStep] = None,
                        entityId: Option[String] = None,
                        utr: Option[String] = None,
                        mtditid: Option[String] = None): IdentifierRequest[_] = {
    IdentifierRequest(
      request = requestWithSession(journeyStep, entityId),
      mtditid = mtditid,
      nino = testNino,
      utr = utr
    )
  }
}
