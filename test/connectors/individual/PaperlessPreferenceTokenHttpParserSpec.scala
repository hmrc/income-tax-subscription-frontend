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

package connectors.individual

import connectors.PaperlessPreferenceTokenResultHttpParser.PaperlessPreferenceTokenResultHttpReads
import models.PaperlessPreferenceTokenResult.{PaperlessPreferenceTokenFailure, PaperlessPreferenceTokenSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class PaperlessPreferenceTokenHttpParserSpec extends UnitTestTrait with EitherValues  {
  val testHttpVerb = "POST"
  val testUri = "/"

  "PaperlessPreferenceTokenHttpParser" when {
    "read" should {
      "parse a CREATED response as a success" in {
        val httpResponse = HttpResponse(CREATED)

        val res = PaperlessPreferenceTokenResultHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe PaperlessPreferenceTokenSuccess
      }

      "parse any other response as a failure" in {
        val httpResponse = HttpResponse(BAD_REQUEST)

        val res = PaperlessPreferenceTokenResultHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe PaperlessPreferenceTokenFailure
      }
    }
  }
}
