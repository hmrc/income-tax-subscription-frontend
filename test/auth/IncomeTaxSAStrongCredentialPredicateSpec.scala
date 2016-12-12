/*
 * Copyright 2016 HM Revenue & Customs
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

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxSAStrongCredentialPredicateSpec extends UnitSpec with WithFakeApplication {

  lazy val predicate = new IncomeTaxSAStrongCredentialPredicate(twoFactorURI)

  "Calling IncomeTaxSAStrongCredentialPredicate with an auth context that has weak credentials" should {
    "result in the page being blocked with a redirect to 2FA" in {
      val authContext = ggUser.weakStrengthUserContext
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe false
    }
  }

  "Calling IncomeTaxSAStrongCredentialPredicate with an auth context that has strong credentials" should {
    "result in page is visible" in {
      val authContext = ggUser.userCL200Context
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe true
    }
  }
}