/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{No, Yes, YesNo}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.{FormError, Mapping}

object YesNoMapping {

  val option_yes = "Yes"
  val option_no = "No"

  def yesNoMapping(yesNoInvalid: Invalid, yesNoEmpty: Option[Invalid] = None): Mapping[YesNo] = of(new Formatter[YesNo] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YesNo] = {
      data.get(key) match {
        case Some(`option_yes`) => Right(Yes)
        case Some(`option_no`) => Right(No)
        case Some(other) if other.nonEmpty => Left(yesNoInvalid.errors.map(e => FormError(key, e.message, e.args)))
        case _ =>
          val err = yesNoEmpty.getOrElse(yesNoInvalid)
          Left(err.errors.map(e => FormError(key, e.message, e.args)))
      }
    }

    override def unbind(key: String, value: YesNo): Map[String, String] = {
      val stringValue = value match {
        case Yes => option_yes
        case No => option_no
      }

      Map(key -> stringValue)
    }
  })

}
