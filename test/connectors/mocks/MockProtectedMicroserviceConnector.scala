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

package connectors.mocks

import config.AppConfig
import connectors.models.subscription.{Both, FEFailureResponse, FERequest, FESuccessResponse}
import connectors.subscription.ProtectedMicroserviceConnector
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.JsValue
import utils.JsonUtils._
import utils.TestConstants

trait MockProtectedMicroserviceConnector extends MockHttp {

  object TestProtectedMicroserviceConnector extends ProtectedMicroserviceConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpPost,
    mockHttpGet
  )

  def setupMockSubscribe()(status: Int, response: JsValue): Unit =
    setupMockHttpPost(url = TestProtectedMicroserviceConnector.subscriptionUrl(""))(status, response)

  def setupMockGetSubscription()(status: Int, response: JsValue): Unit =
    setupMockHttpGet(url = TestProtectedMicroserviceConnector.subscriptionUrl(TestConstants.testNino))(status, response)

  val setupSubscribe = (setupMockSubscribe() _).tupled
  val setupGetSubscription = (setupMockGetSubscription() _).tupled

  val testRequest = FERequest(
    nino = TestConstants.testNino,
    incomeSource = Both,
    accountingPeriodStart = TestConstants.startDate,
    accountingPeriodEnd = TestConstants.endDate,
    cashOrAccruals = "Cash",
    tradingName = "ABC"
  )
  val testId = TestConstants.testMTDID
  val badRequestReason = "Bad request"
  val internalServerErrorReason = "Internal server error"

  val subscribeSuccess = (OK, FESuccessResponse(testId): JsValue)
  val subscribeNone = (OK, FESuccessResponse(None): JsValue)
  val subscribeBadRequest = (BAD_REQUEST, FEFailureResponse(badRequestReason): JsValue)
  val subscribeInternalServerError = (INTERNAL_SERVER_ERROR, FEFailureResponse(internalServerErrorReason): JsValue)

}
