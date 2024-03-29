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

package forms.agent

import forms.agent.submapping.BusinessIncomeSourceMapping
import models.IncomeSourcesStatus
import models.common.BusinessIncomeSource
import play.api.data.Form
import play.api.data.Forms.single

object BusinessIncomeSourceForm {
  val incomeSourceKey = "income-source"

  def businessIncomeSourceForm(incomeSourcesStatus: IncomeSourcesStatus): Form[BusinessIncomeSource] = Form(
    single(
      incomeSourceKey -> BusinessIncomeSourceMapping("agent.income-source.error", incomeSourcesStatus)
    )
  )
}
