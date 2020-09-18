/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.individual.business

import forms.individual.business.AddAnotherBusinessForm.{addAnotherBusiness, addAnotherBusinessForm}
import forms.submapping.YesNoMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils.ErrorValidationUtil
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class AddAnotherBusinessFormSpec extends PlaySpec with GuiceOneAppPerTest {


  "The AddAnotherBusinessForm" should {

    val empty = "error.add_another_business.empty"
    val invalid = "error.add_another_business.invalid"
    val limitReached = "error.add_another_business.limit_reached"

    "validate add another business Yes/No correctly" when {

      val limit = 50
      val currentBusinesses = 3

      "the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = addAnotherBusinessForm(currentBusinesses, limit).bind(emptyInput0)
        emptyTest0.errors must contain(FormError(addAnotherBusiness, empty))
      }

      "the input is empty" in {
        val emptyInput = DataMap.addAnotherBusiness("")
        val emptyTest = addAnotherBusinessForm(currentBusinesses, limit).bind(emptyInput)
        emptyTest.errors must contain(FormError(addAnotherBusiness, empty))
      }

      "the input is invalid" in {
        val invalidInput = DataMap.addAnotherBusiness("α")
        val invalidTest = addAnotherBusinessForm(currentBusinesses, limit).bind(invalidInput)
        invalidTest.errors must contain(FormError(addAnotherBusiness, invalid))
      }

      "The following submission should be valid" in {
        val testYes = DataMap.addAnotherBusiness(YesNoMapping.option_yes)
        addAnotherBusinessForm(currentBusinesses, limit) isValidFor testYes
        val testNo = DataMap.addAnotherBusiness(YesNoMapping.option_no)
        addAnotherBusinessForm(currentBusinesses, limit) isValidFor testNo
      }
    }

    "validate add another business limit correctly" when {
      "they have reach the business limit" in {
        val limit = 50
        val currentBusinesses = 51

          val withinLimitInput = DataMap.addAnotherBusiness(YesNoMapping.option_yes)
          val withinLimitTest = addAnotherBusinessForm(limit, currentBusinesses).bind(withinLimitInput)
          withinLimitTest.value mustNot contain(limitReached)
        }

    }
  }
}
