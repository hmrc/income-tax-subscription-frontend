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

package forms.formatters

import models.DateModel
import play.api.data.FormError

object DateModelMapping {

  type DateModelValidation = Either[Seq[FormError], DateModel]

  // Encapsulation of field ids.
  // We should never send people to the field with id "key".  Only to an editable field.
  // The sole exception to this is the fallback position where all our checks have passed
  // but the values do not parse to a local date for some reason.
  case class HtmlIds(key: String) {
    private val day: String = "dateDay"
    private val month: String = "dateMonth"
    private val year: String = "dateYear"

    val totalDayKey: String = s"$key-$day"
    val totalMonthKey: String = s"$key-$month"
    val totalYearKey: String = s"$key-$year"
  }
}
