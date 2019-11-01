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

package core.forms.submapping

import core.models.{AccountingMethod, AccountingYear, Accruals, Cash, Current, Next}
import play.api.data.Forms.of
import play.api.data.{FormError, Mapping}
import play.api.data.format.Formatter
import play.api.data.validation.Invalid

object AccountingYearMapping {

  val option_current = "CurrentYear"
  val option_next = "NextYear"

  def apply(errInvalid: Invalid, errEmpty: Option[Invalid]): Mapping[AccountingYear] = of(new Formatter[AccountingYear] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountingYear] = {
      data.get(key) match {
        case Some(`option_current`) => Right(Current)
        case Some(`option_next`) => Right(Next)
        case Some(other) if other.nonEmpty => Left(errInvalid.errors.map(e => FormError(key, e.message, e.args)))
        case _ =>
          val err = errEmpty.getOrElse(errInvalid)
          Left(err.errors.map(e => FormError(key, e.message, e.args)))
      }
    }

    override def unbind(key: String, value: AccountingYear): Map[String, String] = {
      val stringValue = value match {
        case Current => option_current
        case Next => option_next
      }

      Map(key -> stringValue)
    }
  })

}
