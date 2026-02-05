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
import common.Constants.ITSASessionKeys
import config.MockConfig
import models.SessionData
import models.audits.IVHandoffAuditing.IVHandoffAuditModel
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAuditingService, MockSessionDataService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.InternalServerException

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentifierActionSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockAuth with MockSessionDataService with MockAuditingService {

  val identifierAction: IdentifierAction = new IdentifierAction(
    authConnector = mockAuth,
    auditingService = mockAuditingService,
    parser = app.injector.instanceOf[BodyParsers.Default],
  )(MockConfig, mockSessionDataService)

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
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), enrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe Some(testUtr)
          request.mtditid mustBe Some(testMTDITID)
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an organisation user who has CL250, has a nino, utr, mtditid" must {
      "create an identifier request with the fetched details" in {
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Organisation), enrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe Some(testUtr)
          request.mtditid mustBe Some(testMTDITID)
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has CL250, has a nino, without a utr enrolment, but has a utr in session" must {
      "create an identifier request with the fetched details" in {
        val sessionData = SessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe Some(testUtr)
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has CL250, has a nino, without a utr enrolment or session and without mtditid" must {
      "create an identifier request with the fetched details" in {
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has higher than CL250" must {
      "create an identifier request with the fetched details" in {
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L500), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an individual user who has CL250, without a nino" must {
      "throw an InternalServerException as the user should have a nino in this scenario" in {
        mockGetAllSessionData()
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
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(User)), ConfidenceLevel.L200), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("")

        verifyAudit(IVHandoffAuditModel(
          handoffReason = "individual",
          currentConfidence = ConfidenceLevel.L200.level,
          minConfidence = 250
        ))
      }
    }
    "authorising an assistant" must {
      "redirect to the cannot use service page" in {
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Individual), emptyEnrolments), Some(Assistant)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.CannotUseServiceController.show().url)
      }
    }
    "authorising an agent" must {
      "redirect to the affinity group error page" in {
        val sessionData = SessionData(Map(
          random() -> JsString(random())
        ))
        mockGetAllSessionData(sessionData)
        mockAuthorise(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino)(
          new~(new~(new~(new~(Some(Agent), emptyEnrolments), Some(User)), ConfidenceLevel.L250), Some(testNino))
        )

        val result: Future[Result] = identifierAction { request =>
          request.nino mustBe testNino
          request.utr mustBe None
          request.mtditid mustBe None
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AffinityGroupErrorController.show.url)
      }
    }
    "authorisation throws an AuthorisationException" must {
      "redirect the user to login" in {
        mockGetAllSessionData()
        mockAuthoriseFailure(EmptyPredicate, affinityGroup and allEnrolments and credentialRole and confidenceLevel and nino) {
          BearerTokenExpired()
        }

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest(method = "GET", path = "/test-url"))

        status(result) mustBe SEE_OTHER
        val url = controllers.individual.matching.routes.HomeController.index.url.replace("/", "%2F")
        redirectLocation(result) mustBe Some(s"/gg/sign-in?continue=$url&origin=${MockConfig.appName}")
      }
    }
  }

  private def random() =
    UUID.randomUUID().toString
}
