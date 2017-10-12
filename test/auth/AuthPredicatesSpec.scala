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

package auth

import _root_.uk.gov.hmrc.http.SessionKeys._
import auth.AuthPredicate.AuthPredicateSuccess
import auth.AuthPredicates._
import controllers.ITSASessionKeys
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockAuthService
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utils.UnitTestTrait

class AuthPredicatesSpec extends UnitTestTrait with MockAuthService with ScalaFutures with EitherValues {

  private def testUser(affinityGroup: Option[AffinityGroup], confidenceLevel: ConfidenceLevel, enrolments: Enrolment*): IncomeTaxSAUser = IncomeTaxSAUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup,
    confidenceLevel
  )

  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxSAUser =
    testUser(affinityGroup, testConfidenceLevel, enrolments: _*)

  val userWithNinoEnrolment = testUser(None, ninoEnrolment)
  val userWithMtditIdEnrolment = testUser(None, mtdidEnrolment)
  val userWithMtditIdEnrolmentAndNino = testUser(None, ninoEnrolment, mtdidEnrolment)
  val userWithUtrButNoNino = testUser(None, utrEnrolment)
  val blankUser = testUser(None, confidenceLevel = ConfidenceLevel.L0)

  val userWithIndividualAffinity = testUser(Some(AffinityGroup.Individual))
  val userWithOrganisationAffinity = testUser(Some(AffinityGroup.Organisation))

  val defaultPredicateUser = testUser(Some(AffinityGroup.Individual), ninoEnrolment)
  val enrolledPredicateUser = testUser(Some(AffinityGroup.Individual), ninoEnrolment, mtdidEnrolment)

  lazy val authorisedRequest = FakeRequest().withSession(
    authToken -> "",
    lastRequestTimestamp -> "",
    ITSASessionKeys.JourneyStateKey -> SignUp.name
  )
  lazy val homelessAuthorisedRequest = FakeRequest().withSession(authToken -> "", lastRequestTimestamp -> "")

  lazy val registrationRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> Registration.name)

  lazy val signUpRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> SignUp.name)

  lazy val userMatchingRequest = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "ninoPredicate" should {
    "return an AuthPredicateSuccess where a nino enrolment exists" in {
      ninoPredicate(FakeRequest())(userWithNinoEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "redirect to resolve nino if nino enrolment does not exist" in {
      await(ninoPredicate(FakeRequest())(blankUser).left.value) mustBe resolveNino
    }

    "return an InternalServerException where a nino enrolment does not exists but a utr enrolment does" in {
      intercept[InternalServerException](await(ninoPredicate(FakeRequest())(userWithUtrButNoNino).left.value))
    }
  }

  "mtdidPredicate" should {
    "return an AuthPredicateSuccess where an mtdid enrolment does not already exist" in {
      mtdidPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the already-enrolled page where an mtdid enrolment already exists" in {
      await(mtdidPredicate(FakeRequest())(userWithMtditIdEnrolment).left.value) mustBe alreadyEnrolled
    }
  }

  "enrolledPredicate" should {
    "return an AuthPredicateSuccess where an mtdid enrolment already exists" in {
      enrolledPredicate(FakeRequest())(userWithMtditIdEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "return a NotFoundException where an mtdid enrolment does not already exist" in {
      intercept[NotFoundException](await(enrolledPredicate(FakeRequest())(blankUser).left.value))
    }
  }


  "timeoutPredicate" should {
    "return an AuthPredicateSuccess where the lastRequestTimestamp is not set" in {
      timeoutPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return an AuthPredicateSuccess where the authToken is set and hte lastRequestTimestamp is set" in {
      timeoutPredicate(authorisedRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the timeout page where the lastRequestTimestamp is set but the auth token is not" in {
      lazy val request = FakeRequest().withSession(lastRequestTimestamp -> "")
      await(timeoutPredicate(request)(blankUser).left.value) mustBe timeoutRoute
    }
  }

  "affinityPredicate" should {
    "return an AuthPredicateSuccess where the affinity group is individual" in {
      affinityPredicate(FakeRequest())(userWithIndividualAffinity).right.value mustBe AuthPredicateSuccess
    }
    "return the wrong-affinity page where the affinity group is organisation" in {
      await(affinityPredicate(FakeRequest())(userWithOrganisationAffinity).left.value) mustBe wrongAffinity
    }
    "return the wrong-affinity page where there is no affinity group" in {
      await(affinityPredicate(FakeRequest())(blankUser).left.value) mustBe wrongAffinity
    }
  }

  "defaultPredicates" should {
    "return an AuthPredicateSuccess where there is a nino, an individual affinity, and an auth token" in {
      defaultPredicates(authorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the wrong-affinity page where there is no affinity group" in {
      await(defaultPredicates(authorisedRequest)(userWithNinoEnrolment).left.value) mustBe wrongAffinity
    }

    "return the no-nino error page where a nino enrolment does not exist" in {
      affinityPredicate(authorisedRequest)(userWithIndividualAffinity).right.value mustBe AuthPredicateSuccess
    }

    "return the timeout page where the lastRequestTimestamp is set but the auth token is not" in {
      lazy val request = FakeRequest().withSession(lastRequestTimestamp -> "")
      await(timeoutPredicate(request)(defaultPredicateUser).left.value) mustBe timeoutRoute
    }

    "return the wrong affinity page where there is no affinity group or nino enrolment" in {
      await(defaultPredicates(authorisedRequest)(blankUser).left.value) mustBe wrongAffinity
    }
  }

  "subscriptionPredicates" should {
    "return an AuthPredicateSuccess where there is a nino, no mtditId, an individual affinity, the home session flag and an auth token" in {
      subscriptionPredicates(authorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the home page where the request session does not contain the GoHomeFlag" in {
      await(subscriptionPredicates(homelessAuthorisedRequest)(defaultPredicateUser).left.value) mustBe homeRoute
    }

    "return the already-enrolled page where an mtdid enrolment already exists" in {
      await(subscriptionPredicates(authorisedRequest)(enrolledPredicateUser).left.value) mustBe alreadyEnrolled
    }
  }

  "enrolledPredicates" should {
    "return an AuthPredicateSuccess where there is a nino, an mtditId, an individual affinity, the home session flag and an auth token" in {
      enrolledPredicates(authorisedRequest)(enrolledPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "throw a NotFoundException where the user is not enrolled" in {
      intercept[NotFoundException](await(enrolledPredicate(authorisedRequest)(defaultPredicateUser).left.value))
    }
  }

  "homePredicates" should {
    "return an AuthPredicateSuccess where there is a nino, no mtditId, an individual affinity and an auth token" in {
      homePredicates(homelessAuthorisedRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return the already-enrolled page where an mtdid enrolment already exists" in {
      await(homePredicates(homelessAuthorisedRequest)(enrolledPredicateUser).left.value) mustBe alreadyEnrolled
    }
  }

  "userMatchingPredicates" should {
    "return an AuthPredicateSuccess where the user has no nino and has the JourneyState flag set to UserMatching" in {
      userMatchingPredicates(userMatchingRequest)(userWithIndividualAffinity).right.value mustBe AuthPredicateSuccess
    }

    "return user to index if the user does not have the JourneyState flag set to UserMatching" in {
      await(userMatchingPredicates(homelessAuthorisedRequest)(userWithIndividualAffinity).left.value) mustBe homeRoute
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

  "ivPredicate" should {
    "return an AuthPredicateSuccess where a user has the Registration journey state and a confidence level of 200 or greater" in {
      ivPredicate(registrationRequest)(defaultPredicateUser).right.value mustBe AuthPredicateSuccess
    }

    "return an AuthPredicateSuccess where a user does not have the Registration journey state" in {
      ivPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the goToIv page where a user has the Registration journey state and a confidence level less than 200" in {
      await(ivPredicate(registrationRequest)(blankUser).left.value) mustBe goToIv
    }
  }
}
