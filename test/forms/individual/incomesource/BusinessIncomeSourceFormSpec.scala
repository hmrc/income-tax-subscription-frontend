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

package forms.individual.incomesource

import forms.individual.incomesource.BusinessIncomeSourceForm.{businessIncomeSourceForm, businessIncomeSourceKey, selfEmployedKey}
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.common.{BusinessIncomeSourceModel, SelfEmployed}
import org.scalatest.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class BusinessIncomeSourceFormSpec  extends PlaySpec with GuiceOneAppPerTest {

  "The AccountingMethodForm" should {
    "transform the request to BusinessIncomeSourceModel" in {
      val testInput = Map(
        businessIncomeSourceKey -> selfEmployedKey
      )
      val expected = BusinessIncomeSourceModel(SelfEmployed)
      val actual = businessIncomeSourceForm().bind(testInput).value

      actual shouldBe Some(expected)
    }

    "show an empty error when the map is empty" in {
      val formWithErrors = businessIncomeSourceForm().bind(DataMap.EmptyMap)

      formWithErrors.errors should contain(
        FormError(businessIncomeSourceKey, "individual.error.business_income_source.invalid")
      )
    }

    "show an foreign property empty error when the map is empty" in {
      val formWithErrors = businessIncomeSourceForm(overseasPropertyEnabled = true).bind(DataMap.EmptyMap)

      formWithErrors.errors should contain(
        FormError(businessIncomeSourceKey, "individual.error.business_income_source_foreignProperty.invalid")
      )
    }

    "show invalid error when the input is invalid" in {
      val testInput = Map(
        businessIncomeSourceKey -> "α"
      )
      val formWithErrors = businessIncomeSourceForm().bind(testInput)

      formWithErrors.errors should contain(
        FormError(businessIncomeSourceKey, "individual.error.business_income_source.invalid")
      )
    }

    "show foreign property invalid error when the input is invalid" in {
      val testInput = Map(
        businessIncomeSourceKey -> "α"
      )
      val formWithErrors = businessIncomeSourceForm(overseasPropertyEnabled = true).bind(testInput)

      formWithErrors.errors should contain(
        FormError(businessIncomeSourceKey, "individual.error.business_income_source_foreignProperty.invalid")
      )
    }

    "validate SelfEmployed inputs" in {
      val testInput = Map(
        businessIncomeSourceKey -> "selfEmployed"
      )

      businessIncomeSourceForm() isValidFor testInput
    }

    "validate UkProperty inputs" in {
      val testInput = Map(
        businessIncomeSourceKey -> "ukProperty"
      )

      businessIncomeSourceForm() isValidFor testInput
    }

    "validate ForeignProperty inputs" in {
      val testInput = Map(
        businessIncomeSourceKey -> "foreignProperty"
      )

      businessIncomeSourceForm() isValidFor testInput
    }
  }

}
