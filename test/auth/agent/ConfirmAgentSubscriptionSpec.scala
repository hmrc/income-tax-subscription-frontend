/*
 * Copyright 2022 HM Revenue & Customs
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

package auth.agent

import auth.individual.AuthPredicate.AuthPredicateSuccess
import auth.individual.AuthPredicates._
import auth.individual.IncomeTaxSAUser
import config.MockConfig
import org.scalatest.EitherValues
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import utilities.individual.TestConstants._
import utilities.individual.{Constants, TestConstants}
import utilities.{ITSASessionKeys, UnitTestTrait}


class ConfirmAgentSubscriptionSpec extends UnitTestTrait with EitherValues {

  implicit val config: MockConfig.type = MockConfig

  lazy val journeyState: ConfirmAgentSubscription.type = ConfirmAgentSubscription

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> journeyState.name,
    ITSASessionKeys.NINO -> testNino,
    ITSASessionKeys.UTR -> testUtr
  )
  lazy val testUser: IncomeTaxSAUser = IncomeTaxSAUser(Enrolments(Set.empty), Some(AffinityGroup.Individual), None, ConfidenceLevel.L200, "")
  lazy val testEnrolledUser: IncomeTaxSAUser = IncomeTaxSAUser(Enrolments(Set(
    Enrolment(Constants.mtdItsaEnrolmentName,
      Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, TestConstants.testMTDID)),
      "Activated"
    ))
  ), Some(AffinityGroup.Individual), None, ConfidenceLevel.L200, "")

  "authPredicates" should {
    s"return $AuthPredicateSuccess when the session contains the correct state" in {
      val res = journeyState.authPredicates.apply(request)(testUser)
      res.value mustBe AuthPredicateSuccess
    }

    s"return $homeRoute when the session is not in the correct state" in {
      val res = journeyState.authPredicates.apply(FakeRequest().withSession((request.session.data - ITSASessionKeys.JourneyStateKey).toSeq: _*))(testUser)
      await(res.left.value) mustBe homeRoute
    }

    s"return $alreadyEnrolledRoute if the user already has a mtdit enrollment" in {
      val res = journeyState.authPredicates.apply(request)(testEnrolledUser)
      await(res.left.value) mustBe alreadyEnrolledRoute
    }
  }

}
