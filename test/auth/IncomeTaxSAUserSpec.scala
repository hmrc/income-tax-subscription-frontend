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

import common.Constants
import connectors.models.{Enrolment, Identifier}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.TestConstants

class IncomeTaxSAUserSpec extends UnitSpec with WithFakeApplication {

  "IncomeTaxSAUser" should {
    lazy val user = IncomeTaxSAUser(
      ggUser.userCL200Context,
        Set(Enrolment(Constants.ninoEnrolmentName,
          Seq(Identifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
          Enrolment.Activated))
    )

    "have the expected NINO 'AB124512C'" in {
      user.nino shouldBe Some(TestConstants.testNino)
    }

    "have the previously logged in time of logged in user" in {
      user.previouslyLoggedInAt shouldBe ggUser.previouslyLoggedInAt
    }
  }
}
