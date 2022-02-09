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

package connectors.individual.subscription.httpparsers

import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourcesResponseHttpReads
import models.common.subscription.{CreateIncomeSourcesFailureResponse, CreateIncomeSourcesSuccess}
import org.scalatest.EitherValues
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait

class CreateIncomeSourcesResponseHttpParserSpec extends UnitTestTrait with EitherValues {
  val testHttpVerb = "POST"
  val testUri = "/"

  "CreateIncomeSourcesResponseHttpReads" when {
    "read" should {
      "parse a correctly formatted OK response as a CreateIncomeSourcesSuccess" in {
        val httpResponse = HttpResponse(NO_CONTENT, "")

        val res = PostCreateIncomeSourcesResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.right.value mustBe CreateIncomeSourcesSuccess()
      }

      "parse any other http status as a CreateIncomeSourcesFailureResponse" in {
        val httpResponse = HttpResponse(BAD_REQUEST, "")

        val res = PostCreateIncomeSourcesResponseHttpReads.read(testHttpVerb, testUri, httpResponse)

        res.left.value mustBe CreateIncomeSourcesFailureResponse(BAD_REQUEST)
      }
    }
  }
}
