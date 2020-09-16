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

import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels
import models.individual.business.AccountingPeriodModel
import models.individual.subscription.SubscriptionSuccess
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import utilities.JsonUtils._
import utilities.{AccountingPeriodUtil, ITSASessionKeys}

object SubscriptionStub extends WireMockMethods {
  def subscriptionURI(nino: String): String = s"/income-tax-subscription/subscription/$nino"
  def subscriptionURIPost(nino: String): String = s"/income-tax-subscription/subscription-v2/$nino"

  def stubSuccessfulPostSubscription(callingPageUri: String): Unit = {
    when(method = POST, uri = subscriptionURIPost(testNino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri))
      .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubSuccessfulPostFailure(callingPageUri: String): Unit = {
    when(method = POST, uri = subscriptionURIPost(testNino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri))
      .thenReturn(Status.BAD_REQUEST)
  }

  def stubIndividualSuccessfulSubscriptionPostWithBoth(callingPageUri: String): Unit = {
    val nino = testNino
    when(method = POST, uri = subscriptionURIPost(nino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri),
      body = successfulIndividualSubscriptionWithBodyBoth(nino = nino))
      .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubSuccessfulSubscriptionPostWithBoth(callingPageUri: String): Unit = {
    val nino = testNino
    when(method = POST, uri = subscriptionURIPost(nino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri),
      body = successfulSubscriptionWithBodyBoth(nino = nino))
    .thenReturn(Status.OK, successfulSubscriptionResponse)
  }

  def stubSuccessfulSubscriptionPostWithProperty(callingPageUri: String): Unit = {
    val nino = testNino
    when(method = POST, uri = subscriptionURIPost(nino), headers = Map(ITSASessionKeys.RequestURI -> callingPageUri),
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

  def successfulSubscriptionWithBodyBusiness(arn: Option[String] = None, nino: String): JsObject = Json.obj(
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
  ) + ("arn" -> arn)

  def successfulSubscriptionWithBodyBoth(arn: Option[String] = None, nino: String): JsObject = Json.obj(
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
  ) + ("arn" -> arn)

  def successfulIndividualSubscriptionWithBodyBoth(arn: Option[String] = None, nino: String): JsObject = {
    val accountingPeriod: AccountingPeriodModel = AccountingPeriodUtil.getCurrentTaxYear
    Json.obj(
      "nino" -> nino,
      "businessIncome" -> Json.obj(
        "tradingName" -> "test business",
        "accountingPeriod" -> Json.obj(
          "startDate" -> Json.obj(
            "day" -> accountingPeriod.startDate.day,
            "month" -> accountingPeriod.startDate.month,
            "year" -> accountingPeriod.startDate.year
          ),
          "endDate" -> Json.obj(
            "day" -> accountingPeriod.endDate.day,
            "month" -> accountingPeriod.endDate.month,
            "year" -> accountingPeriod.endDate.year
          )
        ),
        "accountingMethod" -> "Cash"
      ),
      "propertyIncome" -> Json.obj("accountingMethod" -> "Cash")
    ) + ("arn" -> arn)
  }

  def successfulSubscriptionWithBodyProperty(arn: Option[String] = None, nino: String): JsObject = Json.obj(
    "nino" -> nino,
    "propertyIncome" -> Json.obj("accountingMethod" -> "Cash")
  ) + ("arn" -> arn)


  val successfulSubscriptionResponse = SubscriptionSuccess(testMtdId)
  val failureSubscriptionResponse = Json.obj()
  val successfulNoSubscriptionResponse = Json.obj()
}
