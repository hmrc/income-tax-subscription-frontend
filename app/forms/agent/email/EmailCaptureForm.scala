/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.agent.email

import forms.validation.Constraints.maxLength
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.Forms.{default, single, text}
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.{nonEmpty, pattern}

import scala.util.matching.Regex

object EmailCaptureForm {

  val formKey: String = "email-capture"
  val emailPattern: Regex = """^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)$""".r
  val emailMaxLength: Int = 254

  val emailIsPresent: Constraint[String] = nonEmpty(s"error.agent.$formKey.empty")
  val emailLength: Constraint[String] = maxLength(emailMaxLength, s"error.agent.$formKey.max-length")
  val emailIsCorrectPattern: Constraint[String] = pattern(emailPattern, "email.regex", s"error.agent.$formKey.invalid")

  val form: Form[String] = Form[String](
    single(formKey -> default(text, "").verifying(emailIsPresent andThen emailLength andThen emailIsCorrectPattern))
  )

}
