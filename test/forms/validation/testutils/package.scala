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

import play.api.data.Form


package object testutils {

  implicit class prefixUtil(prefix: String) {
    def `*`(name: String): String = prefix match {
      case "" => name
      case _ => s"$prefix.$name"
    }
  }

  implicit class ErrorValidationUtil[T](testForm: Form[T]) {
    implicit def assert(testFieldName: String): TestTrait[T] = new TestTrait[T] {
      override val form: Form[T] = testForm
      override val fieldName: String = testFieldName
    }
  }

}
