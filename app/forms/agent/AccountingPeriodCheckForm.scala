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

import forms.submapping.YesNoMapping.yesNoMapping
import models.YesNo
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.validation.Invalid

object AccountingPeriodCheckForm {

  val accountingPeriodCheck = "yes-no"

  val accountingPeriodCheckForm: Form[YesNo] = Form(
    single(
      accountingPeriodCheck -> yesNoMapping(
        yesNoInvalid = Invalid("agent.accounting-period-check.invalid")
      )
    )
  )

}
