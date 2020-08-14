/*
 * Copyright 2020 HM Revenue & Customs
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

package utilities.individual

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils


@Singleton
class ImplicitDateFormatterImpl @Inject()(val languageUtils: LanguageUtils) extends ImplicitDateFormatter

trait ImplicitDateFormatter {

  val languageUtils: LanguageUtils

  implicit class LongDate(date: LocalDate)(implicit messages: Messages) {

    def toLongDate: String = {
      languageUtils.Dates.formatDate(org.joda.time.LocalDate.parse(date.toString))(messages)
    }

  }

}