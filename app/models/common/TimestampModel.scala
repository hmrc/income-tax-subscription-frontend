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

package models.common

import play.api.libs.json.{Format, JsPath, JsString, JsValue, Json, Reads, Writes}

import java.time.{Instant, LocalDateTime, ZoneId}

case class TimestampModel(dateTime: java.time.LocalDateTime)

object TimestampModel {
  private val reads: Reads[TimestampModel] =
    ((JsPath \ "$date").read[Long])
      .map[TimestampModel](dateTimeSecs => TimestampModel(
        LocalDateTime.ofInstant(Instant.ofEpochSecond(dateTimeSecs), ZoneId.of("UTC"))))

  private val writes: Writes[TimestampModel] = new Writes[TimestampModel] {
    def writes(model: TimestampModel): JsValue = JsString(model.dateTime.toString())
  }

  implicit val format: Format[TimestampModel] = Format[TimestampModel](reads, writes)
}
