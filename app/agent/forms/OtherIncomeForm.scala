/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.forms

import core.forms.submapping.YesNoMapping
import core.forms.validation.ErrorMessageFactory
import core.models.YesNo
import play.api.data.Form
import play.api.data.Forms.single

object OtherIncomeForm {

  val choice = "choice"

  val otherIncomeForm = Form(
    single[YesNo](
      choice -> YesNoMapping.yesNoMapping(
        yesNoInvalid = ErrorMessageFactory.error("agent.error.other-income.invalid"),
        yesNoEmpty = Some(ErrorMessageFactory.error("agent.error.other-income.empty"))
      )
    )
  )

}
