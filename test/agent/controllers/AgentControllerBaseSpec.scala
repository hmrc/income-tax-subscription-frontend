/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import agent.auth.{AgentRegistration, AgentSignUp, AgentUserMatched, AgentUserMatching}
import agent.controllers
import org.mockito.Mockito
import play.api.data.Form
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import agent.services.mocks.MockAgentAuthService
import agent.utils.TestConstants
import core.controllers.ControllerBaseTrait
import core.utils.JsonUtils
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}

trait AgentControllerBaseSpec extends ControllerBaseTrait with MockAgentAuthService with JsonUtils {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val controllerName: String
  val authorisedRoutes: Map[String, Action[AnyContent]]

  final def authorisationTests(): Unit = {
    authorisedRoutes.foreach {
      case (name, call) =>
        s"Calling the $name action of the $controllerName with an unauthorised user" should {

          lazy val result = call(FakeRequest())

          "return an AuthorisationException" in {
            Mockito.reset(mockAuthService)

            val exception = new InvalidBearerToken()
            mockAuthUnauthorised(exception)

            intercept[AuthorisationException](await(result)) mustBe exception
          }
        }
    }
  }

  implicit class FakeRequestUtil[C](fakeRequest: FakeRequest[C]) {
    implicit def post[T](form: Form[T], data: T): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.post(form.fill(data))

    implicit def post[T](form: Form[T]): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.data.toSeq: _*)

    def addingToSession(newSessions: (String, String)*): FakeRequest[C] = {
      fakeRequest.withSession(fakeRequest.session.data ++: newSessions: _*)
    }

    def removeFromSession(sessionKeys: String*): FakeRequest[C] = {
      FakeRequest().withSession(fakeRequest.session.data.filter { case (k, v) => !sessionKeys.contains(k) }.toSeq: _*)
        .withBody(fakeRequest.body)
    }
  }

  lazy val fakeRequest = FakeRequest()

  lazy val userMatchingRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  lazy val userMatchedRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatched.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr
  )


  lazy val unauthorisedUserMatchedRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatched.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr,
    ITSASessionKeys.UnauthorisedAgentKey -> false.toString
  )

  lazy val unauthorisedUserMatchingRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr,
    ITSASessionKeys.UnauthorisedAgentKey -> true.toString
  )


  lazy val userMatchedRequestNoUtr = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatched.name,
    ITSASessionKeys.NINO -> TestConstants.testNino
  )

  lazy val subscriptionRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr
  )

  lazy val registrationRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentRegistration.name,
    ITSASessionKeys.NINO -> TestConstants.testNino
  )
}
