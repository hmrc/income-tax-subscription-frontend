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

import connectors.models.subscription.{Both, FEFailureResponse, FERequest, FESuccessResponse}
import connectors.subscription.MiddleServiceConnector
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.HttpPost
import utils.JsonUtils._
import utils.TestConstants

trait MockMiddleServiceConnector extends MockHttp {

  lazy val httpPost: HttpPost = mockHttpPost

  object TestMiddleServiceConnector extends MiddleServiceConnector(httpPost)

  def setupMockSubscribe()(status: Int, response: JsValue): Unit =
    setupMockHttpPost(url = TestMiddleServiceConnector.postUrl)(status, response)

  val setupSubscribe = (setupMockSubscribe() _).tupled

  val testRequest = FERequest(TestConstants.testNino, Both)
  val testId = TestConstants.testMTDID
  val badRequestReason = "Bad request"
  val internalServerErrorReason = "Internal server error"

  val subScribeSuccess = (OK, FESuccessResponse(testId): JsValue)
  val subScribeBadRequest = (BAD_REQUEST, FEFailureResponse(badRequestReason): JsValue)
  val subScribeInternalServerError = (INTERNAL_SERVER_ERROR, FEFailureResponse(internalServerErrorReason): JsValue)

}
