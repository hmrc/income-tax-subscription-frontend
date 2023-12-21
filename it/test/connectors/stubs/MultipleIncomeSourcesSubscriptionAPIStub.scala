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

package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.servicemocks.WireMockMethods
import models.common.subscription.CreateIncomeSourcesModel
import play.api.libs.json.{JsValue, Json}

object MultipleIncomeSourcesSubscriptionAPIStub extends WireMockMethods {

  private def signUpUri(nino: String, taxYear: String): String = s"/income-tax-subscription/mis/sign-up/$nino/$taxYear"

  private def createIncomeSourcesUri(mtdbsa: String): String = s"/income-tax-subscription/mis/create/$mtdbsa"

  def stubPostSignUp(nino: String, taxYear: String)(responseCode: Int, response: JsValue = Json.obj("mtdbsa" -> testMtdId)): StubMapping = {
    when(
      method = POST,
      uri = signUpUri(nino, taxYear)
    ).thenReturn(
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscriptionForTaskList(mtdbsa: String, request: CreateIncomeSourcesModel)
                                     (responseCode: Int,
                                      response: JsValue = Json.obj()): StubMapping = {
    when(
      method = POST,
      uri = createIncomeSourcesUri(mtdbsa),
      body = Json.toJson(request)
    ).thenReturn(
      status = responseCode,
      body = response
    )
  }
}
