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

package connectors

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.stubs.MultipleIncomeSourcesSubscriptionAPIStub._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testMtdId, testNino}
import models.common.subscription.SignUpSourcesFailure.{BadlyFormattedSignUpIncomeSourcesResponse, SignUpIncomeSourcesFailureResponse}
import models.common.subscription.SignUpSuccessResponse.SignUpSuccessful
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class MultipleIncomeSourcesSubscriptionConnectorISpec extends ComponentSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val TestMisSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector = app.injector.instanceOf[MultipleIncomeSourcesSubscriptionConnector]

  val testTaxYear: String = "2023-24"

  "MultipleIncomeSourcesSubscription signup" should {
    "return SignUpIncomeSourcesSuccess when valid response is returned" in {
      stubPostSignUp(testNino, testTaxYear)(OK)

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Right(SignUpSuccessful(testMtdId))
    }
    "return BadlyFormattedSignUpIncomeSourcesResponse when the response is malformed" in {
      stubPostSignUp(testNino, testTaxYear)(OK, Json.obj("not" -> "correct"))

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Left(BadlyFormattedSignUpIncomeSourcesResponse)
    }
    "return SignUpIncomeSourcesFailureResponse if the request fails" in {
      stubPostSignUp(testNino, testTaxYear)(INTERNAL_SERVER_ERROR)

      val res = TestMisSubscriptionConnector.signUp(testNino, testTaxYear)

      await(res) mustBe Left(SignUpIncomeSourcesFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }
}
