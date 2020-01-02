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

package incometax.incomesource.forms

import core.forms.submapping.YesNoMapping
import core.forms.validation.ErrorMessageFactory
import core.models.YesNo
import incometax.incomesource.models.AreYouSelfEmployedModel
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}


object AreYouSelfEmployedForm {

  val choice = "choice"

  val choiceMapping: Mapping[YesNo] = YesNoMapping.yesNoMapping(
    yesNoInvalid = ErrorMessageFactory.error("error.are_you_selfemployed.invalid"),
    yesNoEmpty = Some(ErrorMessageFactory.error("error.are_you_selfemployed.empty"))
  )

  val areYouSelfEmployedForm = Form(
    mapping(
      choice -> choiceMapping
    )(AreYouSelfEmployedModel.apply)(AreYouSelfEmployedModel.unapply)
  )

}
