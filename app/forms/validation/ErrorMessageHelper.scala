/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.data.{Field, Form}


object ErrorMessageHelper {

  def getFieldError(form: Form[_], fieldName: String) = {
    val err = form.errors(fieldName)
    err
  }

  def getFieldError(field: Field) = {
    field.errors
  }

  def getFieldError(field: Field, parentForm: Form[_]) = {
    parentForm.errors(field.name)
  }

  def getSummaryErrors(form: Form[_]) = {
    form.errors
  }

}
