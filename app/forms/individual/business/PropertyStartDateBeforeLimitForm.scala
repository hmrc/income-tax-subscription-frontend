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

import forms.submapping.YesNoMapping
import models.YesNo
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.validation.Invalid
import utilities.AccountingPeriodUtil

object PropertyStartDateBeforeLimitForm {

  val startDateBeforeLimit = "start-date-before-limit"

  val startDateBeforeLimitForm: Form[YesNo] = Form(
    single(
      startDateBeforeLimit -> YesNoMapping.yesNoMapping(
        yesNoInvalid = Invalid("error.property.start-date-before-limit.invalid", AccountingPeriodUtil.getStartDateLimit.getYear.toString)
      )
    )
  )

}
