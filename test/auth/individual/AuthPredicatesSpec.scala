/*
 * Copyright 2020 HM Revenue & Customs
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

package auth.individual

import auth.individual.AuthPredicate.AuthPredicateSuccess
import auth.individual.JourneyState._
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.individual.mocks.MockAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys._
import utilities.{ITSASessionKeys, UnitTestTrait}

class AuthPredicatesSpec extends UnitTestTrait with MockAuthService with ScalaFutures with EitherValues {

  val authPredicates: AuthPredicates = new AuthPredicates {}

  import authPredicates._

  private def testUser(affinityGroup: Option[AffinityGroup], credentialRole: Option[CredentialRole], confidenceLevel: ConfidenceLevel,
                       enrolments: Enrolment*): IncomeTaxSAUser = IncomeTaxSAUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup,
    credentialRole = credentialRole,
    confidenceLevel
  )

  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxSAUser =
    testUser(affinityGroup, Some(User), testConfidenceLevel, enrolments: _*)

  val userWithNinoEnrolment: IncomeTaxSAUser = testUser(None, ninoEnrolment)
  val userWithMtditIdEnrolment: IncomeTaxSAUser = testUser(None, mtdidEnrolment)
  val userWithMtditIdEnrolmentAndNino: IncomeTaxSAUser = testUser(None, ninoEnrolment, mtdidEnrolment)
  val userWithUtrButNoNino: IncomeTaxSAUser = testUser(Some(AffinityGroup.Individual), utrEnrolment)
  val blankUser: IncomeTaxSAUser = testUser(None, None, confidenceLevel = ConfidenceLevel.L0)

  val userWithIndividualAffinity: IncomeTaxSAUser = testUser(Some(AffinityGroup.Individual))
  val userWithAgentAffinity: IncomeTaxSAUser = testUser(Some(AffinityGroup.Agent))
  val userWithOrganisationAffinity: IncomeTaxSAUser = testUser(Some(AffinityGroup.Organisation))

  val defaultPredicateUser: IncomeTaxSAUser = testUser(Some(AffinityGroup.Individual), ninoEnrolment)
  val enrolledPredicateUser: IncomeTaxSAUser = testUser(Some(AffinityGroup.Individual), ninoEnrolment, mtdidEnrolment)

  lazy val authorisedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    authToken -> "",
    lastRequestTimestamp -> "",
    ITSASessionKeys.JourneyStateKey -> SignUp.name
  )
  lazy val homelessAuthorisedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(authToken -> "", lastRequestTimestamp -> "")

  lazy val signUpRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> SignUp.name)

  lazy val userMatchingRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "ninoPredicate" should {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    "return an AuthPredicateSuccess where a nino enrolment exists" in {
      AuthPredicates.ninoPredicate(FakeRequest())(userWithNinoEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "redirect to user matching if nino enrolment does not exist" in {
      val res = await(AuthPredicates.ninoPredicate(request)(blankUser).left.value)
      res mustBe (AuthPredicates.userMatching withJourneyState UserMatching)
    }

    "redirect to home if a nino enrolment does not exist but a utr enrolment does" in {
      await(AuthPredicates.ninoPredicate(FakeRequest())(userWithUtrButNoNino).left.value) mustBe AuthPredicates.homeRoute

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
    "return the wrong-affinity page where the affinity group is agent" in {
      await(affinityPredicate(FakeRequest())(userWithAgentAffinity).left.value) mustBe wrongAffinity
    }
    "return the wrong-affinity page where there is no affinity group" in {
      await(affinityPredicate(FakeRequest())(blankUser).left.value) mustBe wrongAffinity
    }

    "return an AuthPredicateSuccess where the affinity group is organisation" in {
      affinityPredicate(FakeRequest())(userWithOrganisationAffinity).right.value mustBe AuthPredicateSuccess
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
    "return an AuthPredicateSuccess where there is an mtditId, the home session flag and an auth token" in {
      enrolledPredicates(authorisedRequest)(testUser(affinityGroup = None, mtdidEnrolment)).right.value mustBe AuthPredicateSuccess
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

    "return to HomeController if there is no nino, but we have a utr" in {
      homePredicates(homelessAuthorisedRequest)(userWithUtrButNoNino).right.value mustBe AuthPredicateSuccess
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
