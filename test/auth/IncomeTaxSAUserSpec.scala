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
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.TestConstants

class IncomeTaxSAUserSpec extends UnitSpec with WithFakeApplication {

  "IncomeTaxSAUser" should {
    lazy val user = IncomeTaxSAUser(
      Enrolments(Set(
        Enrolment(Constants.ninoEnrolmentName,
          Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
          "Activated",
          L50
        )
      )),
      None
    )

    s"have the expected NINO ${TestConstants.testNino}" in {
      user.nino shouldBe Some(TestConstants.testNino)
    }
  }
}
