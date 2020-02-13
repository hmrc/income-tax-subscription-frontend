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

package forms.individual.business

import forms.submapping.YesNoMapping
import forms.validation.ErrorMessageFactory
import models.individual.business.MatchTaxYearModel
import play.api.data.Form
import play.api.data.Forms.mapping

object MatchTaxYearForm {

  val matchTaxYear = "matchToTaxYear"

  val matchTaxYearForm = Form(
    mapping(
      matchTaxYear -> YesNoMapping.yesNoMapping(
        yesNoInvalid = ErrorMessageFactory.error("error.business.match_tax_year.invalid"),
        yesNoEmpty = Some(ErrorMessageFactory.error("error.business.match_tax_year.empty"))
      )
    )(MatchTaxYearModel.apply)(MatchTaxYearModel.unapply)
  )

}
