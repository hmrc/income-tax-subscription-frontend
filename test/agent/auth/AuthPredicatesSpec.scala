/*
 * Copyright 2017 HM Revenue & Customs
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

package agent.auth

import _root_.uk.gov.hmrc.http.SessionKeys._
import agent.auth.AuthPredicate.AuthPredicateSuccess
import agent.auth.AuthPredicates._
import agent.controllers.ITSASessionKeys
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.test.Helpers._
import agent.services.mocks.MockAuthService
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import uk.gov.hmrc.http.NotFoundException
import agent.utils.UnitTestTrait

class AuthPredicatesSpec extends UnitTestTrait with MockAuthService with ScalaFutures with EitherValues {
  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxSAUser = IncomeTaxSAUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup
  )

  val userWithArnEnrolment = testUser(None, arnEnrolment)
  val blankUser = testUser(None)

  val userWithIndividualAffinity = testUser(Some(AffinityGroup.Individual))
  val userWithOrganisationAffinity = testUser(Some(AffinityGroup.Organisation))

  val defaultPredicateUser = testUser(Some(AffinityGroup.Agent), arnEnrolment)

  lazy val authorisedRequest = FakeRequest().withSession(
    authToken -> "",
    lastRequestTimestamp -> "",
    ITSASessionKeys.JourneyStateKey -> SignUp.name
  )
  lazy val homelessAuthorisedRequest = FakeRequest().withSession(authToken -> "", lastRequestTimestamp -> "")

  lazy val registrationRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> Registration.name)

  lazy val signUpRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> SignUp.name)

  lazy val postConfirmationRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> SignUp.name, ITSASessionKeys.Submitted -> "any")

  lazy val userMatchingRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "arnPredicate" should {
    "return an AuthPredicateSuccess where a arn enrolment exists" in {
      arnPredicate(FakeRequest())(userWithArnEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "redirect to no arn if arn enrolment does not exist" in {
      await(arnPredicate(FakeRequest())(blankUser).left.value) mustBe noArnRoute
    }
  }

  "timeoutPredicate" should {
    "return an AuthPredicateSuccess where the lastRequestTimestamp is not set" in {
      timeoutPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return an AuthPredicateSuccess where the authToken is set and hte lastRequestTimestamp is set" in {
      timeoutPredicate(authorisedRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }
  }

  "defaultPredicates" should {
    "return an AuthPredicateSuccess where there is a arn, an agent affinity, and an auth token" in {
      defaultPredicates(authorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the timeout page where the lastRequestTimestamp is set but the auth token is not" in {
      lazy val request = FakeRequest().withSession(lastRequestTimestamp -> "")
      await(timeoutPredicate(request)(defaultPredicateUser).left.value) mustBe timeoutRoute
    }

    "redirect to no arn if arn enrolment does not exist" in {
      await(arnPredicate(FakeRequest())(blankUser).left.value) mustBe noArnRoute
    }
  }

  "subscriptionPredicates" should {
    "return an AuthPredicateSuccess where there is a nino, no mtditId, an agent affinity, the home session flag and an auth token" in {
      subscriptionPredicates(authorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the home where the request session does not contain the GoHomeFlag" in {
      await(subscriptionPredicates(homelessAuthorisedRequest)(defaultPredicateUser).left.value) mustBe homeRoute
    }
  }

  "homePredicates" should {
    "return an AuthPredicateSuccess if the user has an arn and the journey is not marked as complete" in {
      homePredicates(homelessAuthorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the confirmation page if the user has an arn but the the journey is marked as complete" in {
      await(homePredicates(postConfirmationRequest)(defaultPredicateUser).left.value) mustBe confirmationRoute
    }
  }

  "confirmationPredicates" should {
    "return an AuthPredicateSuccess if the user has an arn but the the journey is marked as complete" in {
      confirmationPredicates(postConfirmationRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the not found if the user has an arn and the journey is not marked as complete" in {
      val ex = intercept[NotFoundException] {
        await(confirmationPredicates(homelessAuthorisedRequest)(defaultPredicateUser).left.value)
      }
      ex.message mustBe "auth.AuthPredicates.hasSubmitted"
    }
  }

  "registrationJourneyPredicate" should {
    "return an AuthPredicateSuccess where a user has the JourneyState flag set to Registration" in {
      registrationJourneyPredicate(registrationRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the index page for any other state" in {
      await(registrationJourneyPredicate(FakeRequest())(blankUser).left.value) mustBe homeRoute
    }
  }

  "signUpJourneyPredicate" should {
    "return an AuthPredicateSuccess where a user has the JourneyState flag set to Registration" in {
      signUpJourneyPredicate(signUpRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the index page for any other state" in {
      await(signUpJourneyPredicate(FakeRequest())(blankUser).left.value) mustBe homeRoute
    }
  }

  "userMatchingJourneyPredicate" should {
    "return an AuthPredicateSuccess where a user has the JourneyState flag set to Registration" in {
      userMatchingJourneyPredicate(userMatchingRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the index page for any other state" in {
      await(userMatchingJourneyPredicate(FakeRequest())(blankUser).left.value) mustBe homeRoute
    }
  }


}
