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

package incometax.incomesource.forms

import core.forms.submapping.YesNoMapping
import core.forms.submapping.YesNoMapping._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.TargetIds
import core.models.{No, Yes, YesNo}
import incometax.incomesource.models.RentUkPropertyModel
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.{Form, FormError, Mapping}

object RentUkPropertyForm {

  val rentUkProperty = "rentUkProperty"
  val onlySourceOfSelfEmployedIncome = "onlySourceOfSelfEmployedIncome"

  val rentUkPropertyMapping: Mapping[YesNo] = YesNoMapping.yesNoMapping(
    yesNoInvalid = ErrorMessageFactory.error("error.rent-uk-property.invalid"),
    yesNoEmpty = Some(ErrorMessageFactory.error("error.rent-uk-property.empty"))
  )

  val onlySourceOfSelfEmployedIncomeMapping: Mapping[Option[YesNo]] = of(new Formatter[Option[YesNo]] {

    val invalid: Invalid = ErrorMessageFactory.error(TargetIds(onlySourceOfSelfEmployedIncome), "error.rent-uk-property.only-source-invalid")
    val empty: Invalid = ErrorMessageFactory.error(TargetIds(onlySourceOfSelfEmployedIncome), "error.rent-uk-property.only-source-empty")

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[YesNo]] = {
      data.get(rentUkProperty) match {
        case Some(`option_yes`) =>
          data.get(key) match {
            case Some(`option_yes`) => Right(Some(Yes))
            case Some(`option_no`) => Right(Some(No))
            case Some(other) if other.nonEmpty => Left(invalid.errors.map(e => FormError(key, e.message, e.args)))
            case _ => Left(empty.errors.map(e => FormError(key, e.message, e.args)))
          }
        case _ => Right(None)
      }
    }

    override def unbind(key: String, value: Option[YesNo]): Map[String, String] = {
      val stringValue = value map {
        case Yes => option_yes
        case No => option_no
      }
      Map(key -> stringValue.getOrElse(""))
    }

  })

  val rentUkPropertyForm = Form(
    mapping(
      rentUkProperty -> rentUkPropertyMapping,
      onlySourceOfSelfEmployedIncome -> onlySourceOfSelfEmployedIncomeMapping
    )(RentUkPropertyModel.apply)(RentUkPropertyModel.unapply)
  )

}
