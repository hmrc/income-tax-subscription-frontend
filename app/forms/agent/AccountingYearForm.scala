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

import agent.models.AccountingYearModel
import forms.submapping.AccountingYearMapping
import forms.validation.ErrorMessageFactory
import play.api.data.Form
import play.api.data.Forms.mapping

object AccountingYearForm {

  val accountingYear = "accountingYear"

  val accountingYearForm: Form[AccountingYearModel] = Form(
    mapping(
      accountingYear -> AccountingYearMapping(
        errInvalid = ErrorMessageFactory.error("agent.error.what-year.invalid"),
        errEmpty = Some(ErrorMessageFactory.error("agent.error.what-year.empty"))
      )
    )(AccountingYearModel.apply)(AccountingYearModel.unapply)
  )
}