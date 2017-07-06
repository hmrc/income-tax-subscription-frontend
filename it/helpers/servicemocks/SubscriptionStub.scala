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

package helpers.servicemocks

import connectors.models.subscription.FESuccessResponse
import controllers.ITSASessionKeys
import helpers.IntegrationTestConstants._
import play.api.http.Status

object SubscriptionStub extends WireMockMethods{
  def subscriptionURI(nino: String): String = s"/income-tax-subscription/subscription/$nino"
  val callingPageURI = "/report-quarterly/income-and-expenses/sign-up/check-your-answers"

  def stubSuccessfulSubscription(): Unit = {
    when(method = POST, uri = subscriptionURI(testNino), headers = Map(ITSASessionKeys.RequestURI -> callingPageURI))
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

  val successfulSubscriptionResponse = FESuccessResponse(Some(testMTDID))
  val successfulNoSubscriptionResponse = FESuccessResponse(None)
}
