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

package core.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import core.ITSASessionKeys
import core.auth.{Registration, SignUp, UserMatched, UserMatching}
import core.services.mocks.MockAuthService
import core.utils.TestConstants
import org.mockito.Mockito
import play.api.data.Form
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import usermatching.userjourneys.ConfirmAgentSubscription

trait ControllerBaseSpec extends ControllerBaseTrait with MockAuthService {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val controllerName: String
  val authorisedRoutes: Map[String, Action[AnyContent]]

  final def authorisationTests(): Unit = {
    authorisedRoutes.foreach {
      case (name, call) =>
        s"Calling the $name action of the $controllerName with an unauthorised user" should {
          lazy val result = call(subscriptionRequest)

          "return an AuthorisationException" in {
            Mockito.reset(mockAuthService)

            val exception = new InvalidBearerToken()
            mockAuthUnauthorised(exception)

            intercept[AuthorisationException](await(result)) mustBe exception
          }
        }
    }
  }

  implicit class FakeRequestUtil(fakeRequest: FakeRequest[_]) {
    implicit def post[T](form: Form[T], data: T): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.post(form.fill(data))

    implicit def post[T](form: Form[T]): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.data.toSeq: _*)
  }

  lazy val fakeRequest = FakeRequest()

  lazy val userMatchingRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatching.name
  )

  lazy val userMatchedRequestNoUtr = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatched.name,
    ITSASessionKeys.NINO -> TestConstants.testNino
  )

  lazy val userMatchedRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatched.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr
  )

  lazy val subscriptionRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> SignUp.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr
  )

  lazy val registrationRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> Registration.name,
    ITSASessionKeys.NINO -> TestConstants.testNino
  )

  lazy val confirmAgentSubscriptionRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> ConfirmAgentSubscription.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.AgentReferenceNumber -> TestConstants.testArn
  )

}
