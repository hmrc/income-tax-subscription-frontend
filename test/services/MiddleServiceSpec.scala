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

package services

import connectors.models.subscription.FESuccessResponse
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockMiddleService


class MiddleServiceSpec extends MockMiddleService {

  def call = await(TestMiddleServiceConnector.subscribe(request = testRequest))

  "RegistrationService" should {
    "return the safeId when the subscription is successful" in {
      setupSubscribe(subScribeSuccess)
      val response = call.get
      response.isInstanceOf[FESuccessResponse] shouldBe true
      response.asInstanceOf[FESuccessResponse].id shouldBe testId
    }

    "return the error if subscription fails on bad request" in {
      setupSubscribe(subScribeBadRequest)
      val response = call
      response shouldBe None
    }

    "return the error if subscription fails on internal server error" in {
      setupSubscribe(subScribeInternalServerError)
      val response = call
      response shouldBe None

    }
  }

}
