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

package agent.helpers.servicemocks

import _root_.agent.common.Constants
import incometax.subscription.connectors.GGAdminConnector
import incometax.subscription.models.{KnownFactsRequest, TypeValuePair}
import _root_.agent.helpers.WiremockHelper
import play.api.http.Status
import play.api.libs.json.{JsNull, Json}

object GGAdminStub extends WireMockMethods {

  private def toKnownFactsRequest(nino: String, mtditId: String): KnownFactsRequest = {
    val knownFactNino = TypeValuePair(Constants.mtdItsaEnrolmentIdentifierKey, mtditId)
    val knownFactMtditId = TypeValuePair(Constants.ninoIdentifierKey, nino)
    KnownFactsRequest(List(knownFactNino, knownFactMtditId))
  }

  def stubKnowFactsSuccess(nino: String, mtditId: String): Unit = {
    val model = toKnownFactsRequest(nino, mtditId)
    when(method = POST, uri = GGAdminConnector.addKnownFactsUri, body = model)
      .thenReturn(status = Status.OK, JsNull)
  }

  def stubKnowFactsFailure(nino: String, mtditId: String): Unit = {
    val model = toKnownFactsRequest(nino, mtditId)
    when(method = POST, uri = GGAdminConnector.addKnownFactsUri, body = model)
      .thenReturn(status = Status.BAD_REQUEST, JsNull)
  }

  def verifyKnownFacts(nino: String, mtditId: String, someCount: Option[Int] = None): Unit = {
    val model = toKnownFactsRequest(nino, mtditId)

    WiremockHelper.verifyPost(uri = GGAdminConnector.addKnownFactsUri, optBody = Some(Json.toJson(model).toString), someCount)
  }

}
