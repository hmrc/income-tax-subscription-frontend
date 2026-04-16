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

package models

import models.SubmissionStatus.{handledError, maxSeconds, otherError, success}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, JsSuccess, Json}

class SubmissionStatusSpec extends PlaySpec {

  private val delay = 1000

  private val inProgress = SubmissionStatus.inProgress

  private val data: Map[SubmissionStatus, JsString] = Map(
    inProgress -> JsString("P"),
    success -> JsString("S"),
    handledError -> JsString("H"),
    otherError -> JsString("O")
  )

  Thread.sleep(delay)

  "Convert to/from Json" in {
    data.foreach { entry =>
      val status = entry._1
      val value = entry._2

      val json = Json.toJson(status)
      val obj = Json.fromJson[SubmissionStatus](json)
      obj mustBe JsSuccess(status)

      Json.toJson(status.status) mustBe value
      Json.fromJson[Status](value) mustBe JsSuccess(status.status)
    }
  }

  "Not have expired" in {
    Thread.sleep(delay)
    data.foreach { entry =>
      val status = entry._1
      status.hasExpired mustBe false
    }
  }

  "Have expired (only for InProgress)" in {
    Thread.sleep(delay * maxSeconds)
    data.foreach { entry =>
      val status = entry._1
      status.hasExpired mustBe (status == inProgress)
    }
  }
}
