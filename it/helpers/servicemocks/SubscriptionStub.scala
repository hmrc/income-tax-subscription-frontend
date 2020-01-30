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

package helpers.servicemocks

import core.ITSASessionKeys
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels
import incometax.subscription.models.SubscriptionSuccess
import play.api.http.Status
import play.api.libs.json.Json

object SubscriptionStub extends WireMockMethods {
  def subscriptionURI(nino: String): String = s"/income-tax-subscription/subscription/$nino"
  def subscriptionURIV2(nino: String): String = s"/income-tax-subscription/subscription-v2/$nino"

  def stubSuccessfulSubscription(callingPageUri: String): Unit = {
    when(method = POST, uri = subscriptionURIV2(testNino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri))
      .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubSuccessfulSubscriptionV2WithBoth(callingPageUri: String): Unit = {
    val nino = testNino
    when(method = POST, uri = subscriptionURIV2(nino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri),
      body = successfulSubscriptionWithBodyBoth(nino = nino))
    .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubSuccessfulSubscriptionV2WithProperty(callingPageUri: String): Unit = {
    val nino = testNino
    when(method = POST, uri = subscriptionURIV2(nino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri),
      body = successfulSubscriptionWithBodyProperty(nino = nino))
      .thenReturn(Status.OK, successfulSubscriptionResponse)
  }



  def stubGetSubscriptionFound(): Unit = {
    when(method = GET, uri = subscriptionURI(testNino))
      .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubGetNoSubscription(): Unit = {
    when(method = GET, uri = subscriptionURI(testNino))
      .thenReturn(Status.OK, successfulNoSubscriptionResponse)
  }

  def stubGetSubscriptionFail(): Unit = {
    when(method = GET, uri = subscriptionURI(testNino))
      .thenReturn(Status.INTERNAL_SERVER_ERROR, failureSubscriptionResponse)
  }

  def stubCreateSubscriptionNotFound(callingPageUri: String): Unit = {
    when(method = POST, uri = subscriptionURI(testNino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri))
      .thenReturn(Status.NOT_FOUND)
  }

  def successfulSubscriptionWithBodyBusiness(arn: Option[String] = None, nino: String) = Json.obj(
    "nino" -> nino,
    "businessIncome" -> Json.obj(
      "tradingName" -> "test business",
      "accountingPeriod" -> Json.obj(
        "startDate" -> Json.obj(
          "day" -> IntegrationTestModels.testStartDate.day,
          "month" -> IntegrationTestModels.testStartDate.month,
          "year" -> IntegrationTestModels.testStartDate.year
        ),
        "endDate" -> Json.obj(
          "day" -> IntegrationTestModels.testEndDate.day,
          "month" -> IntegrationTestModels.testEndDate.month,
          "year" -> IntegrationTestModels.testEndDate.year
        )
      ),
      "accountingMethod" -> "Cash"
    ), "selectedTaxYear" -> IntegrationTestModels.testAccountingYearNext
  ) ++ arn.fold(Json.obj())(arn => Json.obj("arn" -> arn))

  def successfulSubscriptionWithBodyBoth(arn: Option[String] = None, nino: String) = Json.obj(
    "nino" -> nino,
    "businessIncome" -> Json.obj(
      "tradingName" -> "test business",
      "accountingPeriod" -> Json.obj(
        "startDate" -> Json.obj(
          "day" -> IntegrationTestModels.testStartDate.day,
          "month" -> IntegrationTestModels.testStartDate.month,
          "year" -> IntegrationTestModels.testStartDate.year
        ),
        "endDate" -> Json.obj(
          "day" -> IntegrationTestModels.testEndDate.day,
          "month" -> IntegrationTestModels.testEndDate.month,
          "year" -> IntegrationTestModels.testEndDate.year
        )
      ),
      "accountingMethod" -> "Cash"
    ),
    "propertyIncome" -> Json.obj("accountingMethod" -> "Cash")
  ) ++ arn.fold(Json.obj())(arn => Json.obj("arn" -> arn))

  def successfulSubscriptionWithBodyProperty(arn: Option[String] = None, nino: String) = Json.obj(
    "nino" -> nino,
    "propertyIncome" -> Json.obj("accountingMethod" -> "Cash")
  ) ++ arn.fold(Json.obj())(arn => Json.obj("arn" -> arn))



  val successfulSubscriptionResponse = SubscriptionSuccess(testMTDID)
  val failureSubscriptionResponse = Json.obj()
  val successfulNoSubscriptionResponse = Json.obj()
}
