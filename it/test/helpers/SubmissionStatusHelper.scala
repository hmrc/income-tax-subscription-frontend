/*
 * Copyright 2018 HM Revenue & Customs
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

package helpers

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub.sessionDataUri
import helpers.WiremockHelper.verifyPost
import models.SubmissionStatus
import org.junit.runners.model.TestTimedOutException
import play.api.libs.json.Json

import java.util.concurrent.TimeUnit

trait SubmissionStatusHelper {
  private val delay = 100;

  def waitUntilStatusIs(expected: SubmissionStatus, timeout: Int = 10 * delay): Unit = {
    def continue(start: Long, current: Long): Boolean = {
      if (current > start + timeout) {
        throw new TestTimedOutException(timeout, TimeUnit.MILLISECONDS)
      } else {
        try {
          verifyPost(sessionDataUri(ITSASessionKeys.SUBMISSION_STATUS), Some(Json.toJson(expected).toString), Some(1))
          false
        } catch {
          case e: Throwable => true
        }
      }
    }

    val start = System.currentTimeMillis()
    while (continue(start, System.currentTimeMillis())) {
      Thread.sleep(delay)
    }
  }
}
