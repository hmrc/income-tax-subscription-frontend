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

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxSACompositePageVisibilityPredicateSpec extends UnitSpec with WithFakeApplication {

  "Calling IncomeTaxSACompositePageVisibilityPredicate with an auth context that has strong credentials and CL50 confidence" should {
    "result in page is visible" in {
      val predicate = new IncomeTaxSACompositePageVisibilityPredicate
      val authContext = ggUser.userCL50Context
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe true
    }
  }

  "Calling IncomeTaxSACompositePageVisibilityPredicate with an auth context that has weak credentials and CL50 confidence" should {
    "result in page is visible" in {
      val predicate = new IncomeTaxSACompositePageVisibilityPredicate
      val authContext = ggUser.weakStrengthUserContext
      val result = predicate(authContext, fakeRequest)
      val pageVisibility = await(result)
      pageVisibility.isVisible shouldBe true
    }
  }

}
