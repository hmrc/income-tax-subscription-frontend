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

import auth.MockAuth
import common.Constants
import config.MockConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentifierActionSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockAuth {

  val identifierAction: IdentifierAction = new IdentifierAction(
    authConnector = mockAuth,
    parser = app.injector.instanceOf[BodyParsers.Default],
    config = app.injector.instanceOf[Configuration],
    env = app.injector.instanceOf[Environment]
  )(MockConfig)

  val testUtr: String = "1234567890"
  val testNino: String = "AA000000A"
  val testMTDITID: String = "XAIT0000000001"

  val enrolments: Enrolments = Enrolments(Set(
    Enrolment(Constants.utrEnrolmentName, Seq(EnrolmentIdentifier(Constants.utrEnrolmentIdentifierKey, testUtr)), "Activated"),
    Enrolment(Constants.mtdItsaEnrolmentName, Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, testMTDITID)), "Activated")
  ))

  val emptyEnrolments: Enrolments = Enrolments(Set())

  "IdentifierAction" when {
    "authorising an individual user who has CL250, has a nino, utr, mtditid" must {
      "create an identifier request with the fetched details" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), enrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe Some(testUtr)
          request.mtditid mustBe Some(testMTDITID)
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an organisation user who has CL250, has a nino, utr, mtditid" must {
      "create an identifier request with the fetched details" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Organisation), enrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe Some(testUtr)
          request.mtditid mustBe Some(testMTDITID)
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has CL250, has a nino, without a utr or mtditid" must {
      "create an identifier request with the fetched details" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has higher than CL250" must {
      "create an identifier request with the fetched details" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L500), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has CL250, without a nino" must {
      "throw an InternalServerException as the user should have a nino in this scenario" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L250), None)
        )

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest())

        intercept[InternalServerException](await(result))
          .message mustBe "[Individual][IdentifierAction] - CL250 User, no nino in retrieval"
      }
    }
    "authorising an individual user with a confidence level less than 250" must {
      "redirect the user to the identification verification service" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L200), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("")
      }
    }
    "authorising an assistant" must {
      "redirect to the cannot use service page" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(Assistant)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.CannotUseServiceController.show().url)
      }
    }
    "authorising an agent" must {
      "redirect to the affinity group error page" in {
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Agent), emptyEnrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AffinityGroupErrorController.show.url)
      }
    }
    "authorisation throws an AuthorisationException" must {
      "redirect the user to login" in {
        mockAuthoriseFailure(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino) {
          BearerTokenExpired()
        }

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest(method = "GET", path = "/test-url"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/bas-gateway/sign-in?continue_url=%2Ftest-url&origin=income-tax-subscription-frontend")
      }
    }
  }

}
