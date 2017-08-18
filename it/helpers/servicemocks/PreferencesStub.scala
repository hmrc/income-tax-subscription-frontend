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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import config.AppConfig
import connectors.preferences.PreferenceFrontendConnector
import helpers.IntegrationTestModels._
import play.api.http.Status
import play.api.i18n.Messages
import play.api.libs.json.Json


object PreferencesStub extends WireMockMethods {
  def preferencesURI(implicit appConfig: AppConfig, messages: Messages) =
    PreferenceFrontendConnector.checkPaperlessUri(returnUrl = PreferenceFrontendConnector.returnUrl(appConfig.baseUrl))

  def stubPaperlessActivated()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> true)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessInactive()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> false)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessPreconditionFail()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse().withStatus(Status.PRECONDITION_FAILED)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessError()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse().withStatus(Status.NOT_FOUND)
    stubFor(mapping.willReturn(response))
  }

}
