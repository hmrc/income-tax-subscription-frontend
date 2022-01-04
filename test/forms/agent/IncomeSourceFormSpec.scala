/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.agent.IncomeSourceForm._
import models.common.IncomeSourceModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import play.api.data.FormBinding.Implicits.formBinding
import play.api.data.{Form, FormError}
import play.api.mvc.Request
import play.api.test.FakeRequest


class IncomeSourceFormSpec extends PlaySpec {

  class FormTest(overseasPropertyEnabled: Boolean,
                 selectedItems: List[String] = List.empty[String],
                 errorTest: Boolean = false) {

    implicit val request: Request[_] = FakeRequest().withFormUrlEncodedBody(
      selectedItems.map(item => s"$incomeSourceKey[]" -> item): _*
    )

    val expectedResult: IncomeSourceModel = IncomeSourceModel(
      selfEmployment = selectedItems.contains(selfEmployedKey),
      ukProperty = selectedItems.contains(ukPropertyKey),
      foreignProperty = selectedItems.contains(overseasPropertyKey)
    )

    val boundForm: Form[IncomeSourceModel] = IncomeSourceForm.incomeSourceForm(
      overseasPropertyEnabled = overseasPropertyEnabled
    ).bindFromRequest()

    val resultValue: Option[IncomeSourceModel] = boundForm.value

    val resultError: Option[FormError] = boundForm.errors.headOption

    if (errorTest) {
      if (overseasPropertyEnabled) {
        resultError mustBe Some(FormError("IncomeSource", "agent.error.income_source_foreignProperty.invalid"))
      } else {
        resultError mustBe Some(FormError("IncomeSource", "agent.error.income_source.invalid"))
      }
    } else {
      resultValue shouldBe Some(expectedResult)
    }

  }

  "The IncomeSource Form" when {
    "overseasPropertyEnabled is set to true" should {
      "transform the request to the form's case class" when {
        "all options have been selected" in new FormTest(
          overseasPropertyEnabled = true,
          selectedItems = List(
            selfEmployedKey, ukPropertyKey, overseasPropertyKey
          )
        )
        "only self employment has been selected" in new FormTest(
          overseasPropertyEnabled = true,
          selectedItems = List(selfEmployedKey)
        )
        "only uk property has been selected" in new FormTest(
          overseasPropertyEnabled = true,
          selectedItems = List(ukPropertyKey)
        )
        "only overseas property has been selected" in new FormTest(
          overseasPropertyEnabled = true,
          selectedItems = List(overseasPropertyKey)
        )
      }
      "return an error" when {
        "no inputs were received" in new FormTest(
          overseasPropertyEnabled = true,
          errorTest = true
        )
      }
    }
    "overseasPropertyEnabled is set to false" should {
      "transform the request to the form's case class" when {
        "self employment and uk property have been selected" in new FormTest(
          overseasPropertyEnabled = false,
          selectedItems = List(
            selfEmployedKey, ukPropertyKey
          )
        )
        "only self employment has been selected" in new FormTest(
          overseasPropertyEnabled = false,
          selectedItems = List(selfEmployedKey)
        )
        "only uk property has been selected" in new FormTest(
          overseasPropertyEnabled = false,
          selectedItems = List(ukPropertyKey)
        )
      }
      "return an error" when {
        "no inputs were received" in new FormTest(
          overseasPropertyEnabled = false,
          errorTest = true
        )
      }
    }
  }
}
