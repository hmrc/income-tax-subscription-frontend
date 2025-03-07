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

package forms.individual.email

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}

class EmailCaptureFormSpec extends PlaySpec with Matchers {

  "EmailCaptureForm" must {
    "return the valid email address" when {
      "bound with a valid email address" in {
        val boundForm: Form[String] = EmailCaptureForm.form.bind(Map(
          EmailCaptureForm.formKey -> validEmail
        ))

        boundForm.errors mustBe Seq.empty[FormError]
        boundForm.value mustBe Some(validEmail)
      }
    }
    "return an error" when {
      "bound with no value" in {
        val boundForm: Form[String] = EmailCaptureForm.form.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(EmailCaptureForm.formKey, s"error.individual.${EmailCaptureForm.formKey}.empty"))
      }
      "bound with an empty value" in {
        val boundForm: Form[String] = EmailCaptureForm.form.bind(Map(
          EmailCaptureForm.formKey -> emptyEmail
        ))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(EmailCaptureForm.formKey, s"error.individual.${EmailCaptureForm.formKey}.empty"))
      }
      "bound with a too long value" in {
        val boundForm: Form[String] = EmailCaptureForm.form.bind(Map(
          EmailCaptureForm.formKey -> tooLongEmail
        ))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(EmailCaptureForm.formKey, s"error.individual.${EmailCaptureForm.formKey}.max-length"))
      }
      "bound with an invalid email" in {
        val boundForm: Form[String] = EmailCaptureForm.form.bind(Map(
          EmailCaptureForm.formKey -> invalidEmail
        ))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError(EmailCaptureForm.formKey, s"error.individual.${EmailCaptureForm.formKey}.invalid", Seq(EmailCaptureForm.emailPattern)))
      }
    }
  }

  lazy val validEmail: String = "a@b.c"
  lazy val invalidEmail: String = "a@b.c."
  lazy val tooLongEmail: String = "a@b.c" * 51
  lazy val emptyEmail: String = ""

}
