/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.individual.business.GlobalCheckYourAnswersForm.{fieldName, form}
import forms.submapping.YesNoMapping
import models.{No, Yes, YesNo}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}

class GlobalCheckYourAnswersFormSpec extends PlaySpec with Matchers {

  "GlobalCheckYourAnswersForm" must {
    "return Yes" when {
      "bound with a yes value" in {
        val boundForm: Form[YesNo] = form.bind(Map(
          fieldName -> YesNoMapping.option_yes
        ))

        boundForm.errors mustBe Seq.empty[FormError]
        boundForm.value mustBe Some(Yes)
      }
    }
    "return No" when {
      "bound with a no value" in {
        val boundForm: Form[YesNo] = form.bind(Map(
          fieldName -> YesNoMapping.option_no
        ))

        boundForm.errors mustBe Seq.empty[FormError]
        boundForm.value mustBe Some(No)
      }
    }
    "return an error" when {
      "bound with an invalid value" in {
        val boundForm: Form[YesNo] = form.bind(Map(
          fieldName -> "invalid"
        ))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(fieldName, "individual.global-check-your-answers.form.error.empty"))
      }
      "bound with no value" in {
        val boundForm: Form[YesNo] = form.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(fieldName, "individual.global-check-your-answers.form.error.empty"))
      }
    }
  }
}
