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

package common

import common.Constants.hmrcAsAgent
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utilities.UnitTestTrait

class ExtractorsSpec extends UnitTestTrait with MockitoSugar with Extractors {

  "getArnFromEnrolments" when {
    "called with no enrolments present" should {
      "return none" in {
        getArnFromEnrolments(Enrolments(Set.empty)) mustBe None
      }
    }
    "called with no value present in an enrolment" should {
      "return some(value)" in {
        val noEnrolments = Enrolments(Set(Enrolment(hmrcAsAgent, Vector.empty, "Activated")))
        getArnFromEnrolments(noEnrolments) mustBe None
      }
    }
    "called with an appropriate value present in an enrolment" should {
      "return some(value)" in {
        val oneEnrolment = Enrolments(Set(
          Enrolment(hmrcAsAgent,
            Vector(
              EnrolmentIdentifier("idKey", "idValue")
            ), "Activated")))
        getArnFromEnrolments(oneEnrolment) mustBe Some("idValue")
      }
    }
    "called with several appropriate values present" should {
      "return the first value" in {
        val oneEnrolment = Enrolments(Set(
          Enrolment(hmrcAsAgent,
            Vector(
              EnrolmentIdentifier("idKey", "idValue1"),
              EnrolmentIdentifier("idKey", "idValue2")
            ), "Activated")))
        getArnFromEnrolments(oneEnrolment) mustBe Some("idValue1")
      }
    }
    "called with several enrolments, one containing several appropriate values present" should {
      "return the first value" in {
        val oneEnrolment = Enrolments(Set(
          Enrolment("NOT_AN_AGENT",
            Vector(
              EnrolmentIdentifier("idKey", "badIdValue1"),
              EnrolmentIdentifier("idKey", "badIdValue2")
            ), "Activated"),
          Enrolment(hmrcAsAgent,
            Vector(
              EnrolmentIdentifier("idKey", "idValue1"),
              EnrolmentIdentifier("idKey", "idValue2")
            ), "Activated")))
        getArnFromEnrolments(oneEnrolment) mustBe Some("idValue1")
      }
    }
  }
}
