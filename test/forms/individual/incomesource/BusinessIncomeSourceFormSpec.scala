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

import models.IncomeSourcesStatus
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.http.InternalServerException

class BusinessIncomeSourceFormSpec extends PlaySpec {

  class FormTest(incomeSourcesStatus: IncomeSourcesStatus, input: String, expectedResult: Either[String, BusinessIncomeSource]) {
    val boundForm: Form[BusinessIncomeSource] = BusinessIncomeSourceForm.businessIncomeSourceForm(
      incomeSourcesStatus = incomeSourcesStatus
    ).bind(
      Map(BusinessIncomeSourceForm.incomeSourceKey -> input)
    )

    expectedResult match {
      case Left(error) =>
        boundForm.errors mustBe Seq(FormError(BusinessIncomeSourceForm.incomeSourceKey, error))
      case Right(value) =>
        boundForm.value mustBe Some(value)
    }
  }

  "businessIncomeSourceForm" should {
    "bind successfully" when {
      "the user selects self employment and it is available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        input = SelfEmployed.toString,
        expectedResult = Right(SelfEmployed)
      )
      "the user selects uk property and it is available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        input = UkProperty.toString,
        expectedResult = Right(UkProperty)
      )
      "the user selects overseas property and it is available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        input = OverseasProperty.toString,
        expectedResult = Right(OverseasProperty)
      )
    }
    "produce a form error" when {
      "self employment is selected but it is not available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        input = SelfEmployed.toString,
        expectedResult = Left("error.business-income-source.uk-property-overseas-property")
      )
      "uk property is selected but it is not available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = true),
        input = UkProperty.toString,
        expectedResult = Left("error.business-income-source.self-employed-overseas-property")
      )
      "overseas property is selected but it is not available" in new FormTest(
        incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = false),
        input = OverseasProperty.toString,
        expectedResult = Left("error.business-income-source.self-employed-uk-property")
      )
    }
    "produce a form error" that {
      "either self employment, uk property or overseas property must be selected" when {
        "there is no input and all options are available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
          input = "",
          expectedResult = Left("error.business-income-source.all-sources")
        )
      }
      "either self employment or uk property must be selected" when {
        "there is no input and overseas property is not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = false),
          input = "",
          expectedResult = Left("error.business-income-source.self-employed-uk-property")
        )
      }
      "either self employment or overseas property must be selected" when {
        "there is no input and uk property is not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = true),
          input = "",
          expectedResult = Left("error.business-income-source.self-employed-overseas-property")
        )
      }
      "either uk property or overseas property must be selected" when {
        "there is no input and self employment is not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = true),
          input = "",
          expectedResult = Left("error.business-income-source.uk-property-overseas-property")
        )
      }
      "self employment must be selected" when {
        "there is no input and uk property and overseas property are not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = false),
          input = "",
          expectedResult = Left("error.business-income-source.self-employed")
        )
      }
      "uk property must be selected" when {
        "there is no input and self employment and overseas property are not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = false),
          input = "",
          expectedResult = Left("error.business-income-source.uk-property")
        )
      }
      "overseas property must be selected" when {
        "there is no input and self employment and uk property are not available" in new FormTest(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = false, overseasPropertyAvailable = true),
          input = "",
          expectedResult = Left("error.business-income-source.overseas-property")
        )
      }
    }
    "produce an exception" when {
      "the form is in a state where there are no options available" in {
        def form: Form[BusinessIncomeSource] = BusinessIncomeSourceForm.businessIncomeSourceForm(
          incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = false, overseasPropertyAvailable = false)
        ).bind(Map.empty[String, String])

        intercept[InternalServerException](form).message mustBe "[BusinessIncomeSourceMapping][apply] - Unexpected state of income sources available"
      }
    }
  }

}
