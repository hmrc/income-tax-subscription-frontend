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

import models.common.ForeignProperty.FOREIGN_PROPERTY
import models.common.SelfEmployed.SELF_EMPLOYED
import models.common.UkProperty.UK_PROPERTY
import models.common.{BusinessIncomeSource, ForeignProperty, SelfEmployed, UkProperty}
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.{FormError, Mapping}

object BusinessIncomeSourceMapping {
  def apply(errInvalid: Invalid, errEmpty: Option[Invalid]): Mapping[BusinessIncomeSource] = of(new Formatter[BusinessIncomeSource] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BusinessIncomeSource] = {
      data.get(key) match {
        case Some(SELF_EMPLOYED) => Right(SelfEmployed)
        case Some(UK_PROPERTY) => Right(UkProperty)
        case Some(FOREIGN_PROPERTY) => Right(ForeignProperty)
        case Some(other) if other.nonEmpty => Left(errInvalid.errors.map(e => FormError(key, e.message, e.args)))
        case _ =>
          val err = errEmpty.getOrElse(errInvalid)
          Left(err.errors.map(e => FormError(key, e.message, e.args)))
      }
    }

    override def unbind(key: String, value: BusinessIncomeSource): Map[String, String] = {
      val stringValue = value match {
        case SelfEmployed => SELF_EMPLOYED
        case UkProperty => UK_PROPERTY
        case ForeignProperty => FOREIGN_PROPERTY
      }

      Map(key -> stringValue)
    }
  })
}
