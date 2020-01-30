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

package forms.agent

import agent.models.AccountingMethodModel
import forms.submapping.AccountingMethodMapping
import forms.validation.ErrorMessageFactory
import play.api.data.Form
import play.api.data.Forms.mapping

object AccountingMethodForm {

  val accountingMethod = "accountingMethod"

  val accountingMethodForm = Form(
    mapping(
      accountingMethod -> AccountingMethodMapping(
        errInvalid = ErrorMessageFactory.error("agent.error.accounting-method.invalid"),
        errEmpty = Some(ErrorMessageFactory.error("agent.error.accounting-method.empty"))
      )
    )(AccountingMethodModel.apply)(AccountingMethodModel.unapply)
  )

}