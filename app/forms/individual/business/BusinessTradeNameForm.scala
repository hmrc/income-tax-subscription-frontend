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

import forms.validation.Constraints.{maxLength, nonEmpty, validateChar}
import models.individual.business.BusinessTradeNameModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import forms.validation.utils.ConstraintUtil.{ConstraintUtil, constraint}
import forms.validation.utils.MappingUtil.trimmedText

object BusinessTradeNameForm {

  val businessTradeName: String = "businessTradeName"

  val businessTradeNameMaxLength = 160

  val tradeNameEmpty: Constraint[String] = nonEmpty("error.business_trade_name.empty")
  val nameTooLong: Constraint[String] = maxLength(businessTradeNameMaxLength, "error.business_trade_name.maxLength")
  val tradeNameInvalidCharacters: Constraint[String] = validateChar("error.business_trade_name.invalid")

  def hasDuplicateTradeNames(excludedNames: Seq[BusinessTradeNameModel]): Constraint[String] = constraint[String] { tradeName =>
    if (excludedNames.exists(_.businessTradeName == tradeName)) Invalid("error.business_trade_name.duplicate")
    else Valid
  }

  def businessTradeNameValidationForm(excludedBusinessTradeNames: Seq[BusinessTradeNameModel]): Form[BusinessTradeNameModel] = Form(
    mapping(
      businessTradeName -> trimmedText.verifying(
        tradeNameEmpty andThen nameTooLong andThen tradeNameInvalidCharacters andThen hasDuplicateTradeNames(excludedBusinessTradeNames)
      )
    )(BusinessTradeNameModel.apply)(BusinessTradeNameModel.unapply)
  )
}
