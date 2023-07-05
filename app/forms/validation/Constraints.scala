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

package forms.validation

import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.Patterns
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.util.{Failure, Success, Try}

object Constraints {

  val nonEmpty: String => Constraint[String] = msgKey => constraint[String](
    x => if (x.isEmpty) Invalid(msgKey) else Valid
  )

  val isNumber: String => Constraint[String] = msgKey => constraint[String](
    x => Try(x.toInt) match {
      case Failure(_) => Invalid(msgKey)
      case Success(_) => Valid
    }
  )

  val maxLength: (Int, String) => Constraint[String] = (length, msgKey) => constraint[String](
    x => if (x.trim.length > length) Invalid(msgKey) else Valid
  )

  val invalidFormat: String => Constraint[String] = msgKey => constraint[String](
    x => if (Patterns.validText(x.trim)) Valid else Invalid(msgKey)
  )

  val emptyNino: Constraint[String] = nonEmpty("error.nino.empty")

  // N.B. this regex is updated to force the user to also enter the suffix
  // the suffix is required because the service we currently call to perform the lookup does not remove it safely
  // and will break if we do not send down the full nino
  val ninoRegex = """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]$"""

  val validateNino: Constraint[String] = {
    constraint[String](nino => if (nino.filterNot(_.isWhitespace).matches(ninoRegex)) Valid else Invalid("error.nino.invalid"))
  }

}
