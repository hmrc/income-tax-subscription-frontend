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

import auth.MockAuth
import config.MockConfig
import models.SessionData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.SessionDataService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentifierActionSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockAuth {

  private val mockSessionDataService = mock[SessionDataService]

  private def random() =
    UUID.randomUUID().toString

  private val sessionData = SessionData(Map(
    random() -> JsString(random())
  ))

  when(mockSessionDataService.getAllSessionData()(any(), any())).thenReturn(
    Future.successful(sessionData)
  )

  val identifierAction: IdentifierAction = new IdentifierAction(
    authConnector = mockAuth,
    parser = app.injector.instanceOf[BodyParsers.Default],
    sessionDataService = mockSessionDataService,
    appConfig = MockConfig
  )

  "IdentifierAction" when {
    "authorising an agent with an agent reference number" must {
      "create an identifier request with the agent reference number and continue the request" in {
        mockAuthorise(EmptyPredicate, Retrievals.affinityGroup and Retrievals.allEnrolments)(
          new~(Some(AffinityGroup.Agent), Enrolments(Set(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier(key = "AgentReferenceNumber", "test-arn")), state = "Activated"))))
        )

        val result: Future[Result] = identifierAction { request =>
          request.arn mustBe "test-arn"
          request.sessionData mustBe sessionData
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorising an agent without an agent reference number" must {
      "return a redirect to the not enrolled for agent services page" in {
        mockAuthorise(EmptyPredicate, Retrievals.affinityGroup and Retrievals.allEnrolments)(
          new~(Some(AffinityGroup.Agent), Enrolments(Set.empty))
        )

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.NotEnrolledAgentServicesController.show.url)
      }
    }
    "authorising a non agent" must {
      "return a redirect to the not enrolled for agent services page" in {
        mockAuthorise(EmptyPredicate, Retrievals.affinityGroup and Retrievals.allEnrolments)(
          new~(Some(AffinityGroup.Individual), Enrolments(Set(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier(key = "AgentReferenceNumber", "test-arn")), state = "Activated"))))
        )

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.NotEnrolledAgentServicesController.show.url)
      }
    }
    "an AuthorisationException is thrown from auth" must {
      "return a redirect to login" in {
        mockAuthoriseFailure(EmptyPredicate, Retrievals.affinityGroup and Retrievals.allEnrolments) {
          BearerTokenExpired()
        }

        val result: Future[Result] = identifierAction { _ =>
          Results.Ok
        }(FakeRequest(method = "GET", path = "/test-url"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/gg/sign-in?continue=%2Ftest-url&origin=${MockConfig.appName}")
      }
    }
  }
}
