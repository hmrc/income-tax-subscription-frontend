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

import forms.submapping.YesNoMapping
import models.YesNo
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.validation.Invalid

object CaptureConsentForm {

  val fieldName: String = "yes-no"

  implicit val mapping: YesNoMapping.type = YesNoMapping

  val captureConsentForm: Form[YesNo] = Form(
    single(
      fieldName -> YesNoMapping.yesNoMapping(
        yesNoInvalid = Invalid("individual.capture-consent.form-error")
      )
    )
  )
}
