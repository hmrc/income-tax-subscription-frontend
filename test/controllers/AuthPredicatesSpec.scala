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

package controllers

import auth.{AuthPredicates, IncomeTaxSAUser}
import AuthPredicates._
import auth.AuthPredicate.AuthPredicateSuccess
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockAuthService
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import uk.gov.hmrc.play.http.NotFoundException
import utils.UnitTestTrait

class AuthPredicatesSpec extends UnitTestTrait with MockAuthService with ScalaFutures with EitherValues {
  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxSAUser = IncomeTaxSAUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup
  )

  val userWithNinoEnrolment = testUser(None, ninoEnrolment)
  val userWithMtditIdEnrolment = testUser(None, mtdidEnrolment)
  val userWithNoEnrolments = testUser(None)

  "ninoPredicate" should {
    "return an AuthPredicateSuccess where a nino enrolment exists" in {
      ninoPredicate(FakeRequest())(userWithNinoEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "return the no-nino error page where a nino enrolment does not exist" in {
      await(ninoPredicate(FakeRequest())(userWithNoEnrolments).left.value) mustBe noNino
    }
  }

  "mtdidPredicate" should {
    "return an AuthPredicateSuccess where an mtdid enrolment does not already exist" in {
      mtdidPredicate(FakeRequest())(userWithNoEnrolments).right.value mustBe AuthPredicateSuccess
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
      intercept[NotFoundException](await(enrolledPredicate(FakeRequest())(userWithNoEnrolments).left.value))
    }
  }

  "goHomePredicate" should {
    "return an AuthPredicateSuccess where the request session contains the GoHome flag" in {
      val request = FakeRequest().withSession(ITSASessionKeys.GoHome -> "et")
      goHomePredicate(request)(userWithNoEnrolments).right.value mustBe AuthPredicateSuccess
    }
  }
}
