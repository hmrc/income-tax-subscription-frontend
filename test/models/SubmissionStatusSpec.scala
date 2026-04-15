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

import models.SubmissionStatus.limit
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class SubmissionStatusSpec extends PlaySpec {

  private val delay = 1000

  private val inProgress = InProgress

  Thread.sleep(delay)

  "Should convert to/from Json" in {
    val data: Seq[SubmissionStatus] = Seq(
      inProgress,
      Success,
      HandledError,
      OtherError
    )

    data.foreach { status =>
      val json = Json.toJson(status)
      val obj = Json.fromJson[SubmissionStatus](json)

      obj mustBe JsSuccess(status)
    }
  }

  "has not expired" in {
    Thread.sleep(delay)
    inProgress.hasExpired mustBe false
  }

  "has expired" in {
    Thread.sleep(delay * limit)
    inProgress.hasExpired mustBe true
  }
}
