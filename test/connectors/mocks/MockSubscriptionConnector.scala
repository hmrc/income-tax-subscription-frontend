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
import connectors.subscription.SubscriptionConnector
import forms.{AccountingPeriodPriorForm, IncomeSourceForm, OtherIncomeForm}
import models._
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.JsValue
import utils.JsonUtils._
import utils.TestConstants

trait MockSubscriptionConnector extends MockHttp {

  object TestSubscriptionConnector extends SubscriptionConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpPost,
    mockHttpGet
  )

  def setupMockSubscribe(request: Option[FERequest] = None)(status: Int, response: JsValue): Unit =
    setupMockHttpPost(url = TestSubscriptionConnector.subscriptionUrl(""), request)(status, response)

  def setupMockGetSubscription(nino: Option[String] = None)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(
      url = nino.fold(None: Option[String])(nino => TestSubscriptionConnector.subscriptionUrl(nino))
    )(status, response)

  def setupSubscribe(request: Option[FERequest] = None) = (setupMockSubscribe(request) _).tupled

  def setupGetSubscription(nino: Option[String] = None) = (setupMockGetSubscription(nino) _).tupled

  val testRequest = FERequest(
    nino = TestConstants.testNino,
    incomeSource = Both,
    accountingPeriodStart = TestConstants.startDate,
    accountingPeriodEnd = TestConstants.endDate,
    cashOrAccruals = "Cash",
    tradingName = "ABC"
  )

  val testSummaryData = SummaryModel(
    incomeSource = IncomeSourceModel(IncomeSourceForm.option_both),
    otherIncome = OtherIncomeModel(OtherIncomeForm.option_no),
    accountingPeriodPrior = AccountingPeriodPriorModel(AccountingPeriodPriorForm.option_no),
    accountingPeriod = AccountingPeriodModel(TestConstants.startDate, TestConstants.endDate),
    businessName = BusinessNameModel("ABC"),
    accountingMethod = AccountingMethodModel("Cash"),
    terms = TermModel(true)
  )

  val testId = TestConstants.testMTDID
  val badRequestReason = "Bad request"
  val internalServerErrorReason = "Internal server error"

  val subscribeSuccess = (OK, FESuccessResponse(testId): JsValue)
  val subscribeNone = (OK, FESuccessResponse(None): JsValue)
  val subscribeBadRequest = (BAD_REQUEST, FEFailureResponse(badRequestReason): JsValue)
  val subscribeInternalServerError = (INTERNAL_SERVER_ERROR, FEFailureResponse(internalServerErrorReason): JsValue)

}
