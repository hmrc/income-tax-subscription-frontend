/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.auth

import agent.common.Constants
import agent.utils.TestConstants
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxSAUserSpec extends UnitSpec with WithFakeApplication {

  "IncomeTaxSAUser" should {
    val confidenceLevel = ConfidenceLevel.L50

    lazy val user = IncomeTaxAgentUser(
      Enrolments(Set(
        Enrolment(Constants.agentServiceEnrolmentName,
          Seq(EnrolmentIdentifier(Constants.agentServiceIdentifierKey, TestConstants.testARN)),
          "Activated"
        )
      )),
      None,
      confidenceLevel
    )

    s"have the expected ARN '${TestConstants.testARN}'" in {
      user.arn shouldBe Some(TestConstants.testARN)
    }

  }
}
