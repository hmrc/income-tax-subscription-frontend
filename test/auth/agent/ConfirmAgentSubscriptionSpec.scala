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

package auth.agent

import auth.individual.AuthPredicate.AuthPredicateSuccess
import auth.individual.AuthPredicates._
import auth.individual.IncomeTaxSAUser
import config.MockConfig
import config.featureswitch.FeatureSwitching
import org.scalatest.EitherValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import utilities.individual.TestConstants._
import utilities.individual.{Constants, TestConstants}
import utilities.{ITSASessionKeys, UnitTestTrait}


class ConfirmAgentSubscriptionSpec extends UnitTestTrait with FeatureSwitching with MockConfig with EitherValues {

  implicit val config = MockConfig

  lazy val journeyState = ConfirmAgentSubscription

  journeyState.featureSwitch foreach { featureSwitch =>
    "isEnabled" should {
      s"return true if the config for ${featureSwitch.displayText} is enabled" in {
        enable(featureSwitch)
        journeyState.isEnabled mustBe true
      }
      s"return false if the config for ${featureSwitch.displayText} is disabled" in {
        disable(featureSwitch)
        journeyState.isEnabled mustBe false
      }
    }
  }

  lazy val request = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> journeyState.name,
    ITSASessionKeys.NINO -> testNino,
    ITSASessionKeys.UTR -> testUtr
  )
  lazy val testUser = IncomeTaxSAUser(Enrolments(Set.empty), Some(AffinityGroup.Individual), None, ConfidenceLevel.L200, "")
  lazy val testEnrolledUser = IncomeTaxSAUser(Enrolments(Set(
    Enrolment(Constants.mtdItsaEnrolmentName,
      Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, TestConstants.testMTDID)),
      "Activated"
    ))
  ), Some(AffinityGroup.Individual), None, ConfidenceLevel.L200,"")

  "authPredicates" should {
    s"return $AuthPredicateSuccess when the session contains the correct state" in {
      val res = journeyState.authPredicates.apply(request)(testUser)
      res.right.value mustBe AuthPredicateSuccess
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
