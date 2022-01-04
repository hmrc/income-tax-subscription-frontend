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

package forms.submapping

import models.{AccountingMethod, Accruals, Cash}
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.{FormError, Mapping}


object AccountingMethodMapping {

  val option_cash = "Cash"
  val option_accruals = "Accruals"

  def apply(errInvalid: Invalid, errEmpty: Option[Invalid]): Mapping[AccountingMethod] = of(new Formatter[AccountingMethod] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountingMethod] = {
      data.get(key) match {
        case Some(`option_cash`) => Right(Cash)
        case Some(`option_accruals`) => Right(Accruals)
        case Some(other) if other.nonEmpty => Left(errInvalid.errors.map(e => FormError(key, e.message, e.args)))
        case _ =>
          val err = errEmpty.getOrElse(errInvalid)
          Left(err.errors.map(e => FormError(key, e.message, e.args)))
      }
    }

    override def unbind(key: String, value: AccountingMethod): Map[String, String] = {
      val stringValue = value match {
        case Cash => option_cash
        case Accruals => option_accruals
      }

      Map(key -> stringValue)
    }
  })

}
