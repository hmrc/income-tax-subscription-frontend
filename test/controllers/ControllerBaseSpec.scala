/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import auth.individual.{ClaimEnrolment, SignUp, UserMatching}
import org.mockito.Mockito
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import services.individual.mocks.MockAuthService
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import utilities.individual.TestConstants
import utilities.{ITSASessionKeys, UnitTestTrait}

trait ControllerBaseSpec extends UnitTestTrait with MockAuthService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val controllerName: String
  val authorisedRoutes: Map[String, Action[AnyContent]]

  final def authorisationTests(): Unit = {
    authorisedRoutes.foreach {
      case (name, call) =>
        s"Calling the $name action of the $controllerName with an unauthorised user" should {
          lazy val result = call(subscriptionRequest)

          "return an AuthorisationException" in {
            Mockito.reset(mockAuthService)

            val exception = InvalidBearerToken()
            mockAuthUnauthorised(exception)

            intercept[AuthorisationException](await(result)) mustBe exception
          }
        }
    }
  }

  implicit class FakeRequestUtil(fakeRequest: FakeRequest[_]) {
    implicit def post[T](form: Form[T], data: T): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.post(form.fill(data))

    implicit def postInvalid[T, I](form: Form[T], data: I): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.mapping.key -> data.toString)

    implicit def post[T](form: Form[T]): FakeRequest[AnyContentAsFormUrlEncoded] =
      fakeRequest.withFormUrlEncodedBody(form.data.toSeq: _*)
  }

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val userMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> UserMatching.name
  )

  lazy val subscriptionRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> SignUp.name,
    ITSASessionKeys.NINO -> TestConstants.testNino,
    ITSASessionKeys.UTR -> TestConstants.testUtr,
    ITSASessionKeys.SPSEntityId -> TestConstants.testSpsEntityId
  )

  lazy val claimEnrolmentRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> ClaimEnrolment.name,
    ITSASessionKeys.NINO -> TestConstants.testNino
  )

}
