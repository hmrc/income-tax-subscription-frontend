/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent

import auth.agent.{AgentSignUp, AgentUserMatching}
import common.Constants.ITSASessionKeys
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.agent.mocks.MockAgentAuthService
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import utilities.{UnitTestTrait, UserMatchingSessionUtil}

import scala.language.implicitConversions

trait AgentControllerBaseSpec extends UnitTestTrait with MockAgentAuthService {

  implicit val system: ActorSystem = ActorSystem()

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

    implicit def postInvalid[T, I](form: Form[T], data: I): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.mapping.key -> data.toString)

    implicit def post[T](form: Form[T]): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.data.toSeq: _*)
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
        .withMethod(POST)

    def addingToSession(newSessions: (String, String)*): FakeRequest[C] = {
      fakeRequest.withSession(fakeRequest.session.data ++: newSessions: _*)
    }

    def removeFromSession(sessionKeys: String*): FakeRequest[C] = {
      FakeRequest().withSession(fakeRequest.session.data.filter { case (k, _) => !sessionKeys.contains(k) }.toSeq: _*)
        .withBody(fakeRequest.body)
    }
  }

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val userMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  lazy val agentSignUpRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

  lazy val unauthorisedUserMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name,
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

  lazy val subscriptionRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  ).withMethod("POST")

  lazy val subscriptionRequestWithName: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    UserMatchingSessionUtil.firstName -> "FirstName",
    UserMatchingSessionUtil.lastName -> "LastName",
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

  lazy val subscriptionRequestWithNameNextYearOnly: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
    UserMatchingSessionUtil.firstName -> "FirstName",
    UserMatchingSessionUtil.lastName -> "LastName",
    ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"
  )

}
