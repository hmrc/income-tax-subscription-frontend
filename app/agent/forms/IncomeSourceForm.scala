/*
 * Copyright 2019 HM Revenue & Customs
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

import core.forms.validation.ErrorMessageFactory
import incometax.subscription.models._
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

object IncomeSourceForm {

  val incomeSource = "incomeSource"
  val option_business = IncomeSourceType.business
  val option_property = IncomeSourceType.property
  val option_both = IncomeSourceType.both
  val option_other = IncomeSourceType.other

  val incomeSourceError: Seq[FormError] = ErrorMessageFactory.formError(incomeSource, "agent.error.income_source.invalid")

  private val formatter: Formatter[IncomeSourceType] = new Formatter[IncomeSourceType] {

    import IncomeSourceType._

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], IncomeSourceType] = {
      data.get(key) match {
        case Some(`business`) => Right(Business)
        case Some(`property`) => Right(Property)
        case Some(`both`) => Right(Both)
        case Some(`other`) => Right(Other)
        case _ => Left(incomeSourceError)
      }
    }

    override def unbind(key: String, value: IncomeSourceType): Map[String, String] = {
      val stringValue = value match {
        case Business => business
        case Property => property
        case Both => both
        case Other => other
      }

      Map(key -> stringValue)
    }
  }

  val incomeSourceForm: Form[IncomeSourceType] = Form(
    single(
      incomeSource -> of(formatter)
    )
  )

}
