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

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import utilities.agent.{Constants, TestConstants}

class IncomeTaxSAUserSpec extends WordSpecLike with Matchers with OptionValues with GuiceOneServerPerTest {

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
