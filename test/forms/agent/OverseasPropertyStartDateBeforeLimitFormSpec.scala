/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.agent

import forms.agent.OverseasPropertyStartDateBeforeLimitForm._
import forms.submapping.YesNoMapping
import models._
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}
import utilities.AccountingPeriodUtil
class OverseasPropertyStartDateBeforeLimitFormSpec extends PlaySpec {

  val form: Form[YesNo] = overseasPropertyStartDateBeforeLimitForm

  "overseasPropertyStartDateBeforeLimitForm" should {
    "bind successfully" when {
      "a start date before limit answer 'Yes'" in {
        val testInput = Map(startDateBeforeLimit -> YesNoMapping.option_yes)
        val expected = Yes

        val actual = form.bind(testInput).value

        actual mustBe Some(expected)
      }
      "a start date before limit answer 'No'" in {
        val testInput = Map(startDateBeforeLimit -> YesNoMapping.option_no)
        val expected = No

        val actual = form.bind(testInput).value

        actual mustBe Some(expected)
      }
    }
    "fail to bind" when {
      "start date before limit is missing" in {
        val testInput = Map.empty[String, String]
        val expectedError: FormError = FormError(
          key = startDateBeforeLimit,
          message = s"agent.error.$errorContext.$startDateBeforeLimit.invalid",
          args = Seq(AccountingPeriodUtil.getStartDateLimit.getYear.toString)
        )

        form.bind(testInput).errors mustBe Seq(expectedError)
      }
    }
  }
}
