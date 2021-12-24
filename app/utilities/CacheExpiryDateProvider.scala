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

package utilities

import org.joda.time.DateTime
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.{Inject, Singleton}

@Singleton
class CacheExpiryDateProvider @Inject()(val languageUtils: LanguageUtils) {

  val cacheRetentionDays = 30

  def format(dateTime: java.time.LocalDateTime)(implicit messages: Messages): String = {
    languageUtils.Dates.formatEasyReadingTimestamp(Option(dateTime), "")(messages)
      .replaceFirst("^.*, ", "").replaceFirst(" (?=\\d)", ", ")
  }

  def expiryDateOf(dateTime: java.time.LocalDateTime)(implicit messages: Messages): String = {
    format(dateTime.plusDays(cacheRetentionDays))
  }
}
