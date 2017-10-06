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
import common.Constants._
import config.AppConfig
import connectors.httpparsers.PaperlessPreferenceHttpParser._
import connectors.preferences.PreferenceFrontendConnector
import helpers.IntegrationTestConstants._
import play.api.http.Status
import play.api.i18n.Messages
import play.api.libs.json.Json


object PreferencesStub extends WireMockMethods {

  val newPreferencesUrl = s".*/paperless/activate-from-token/$preferencesServiceKey.*"

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

  def stubPaperlessInactiveWithUri()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> false, redirectUserTo -> testUrl)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessPreconditionFail()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse()
      .withStatus(Status.PRECONDITION_FAILED)
      .withBody(preconditionFailedJson.toString())
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessError()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse().withStatus(Status.NOT_FOUND)
    stubFor(mapping.willReturn(response))
  }

//  Feature Switch methods for new Preferences sign-up solution

  def newStubPaperlessActivated()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val rBody = Json.obj("optedIn" -> true)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessInactiveWithUri()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val rBody = Json.obj("optedIn" -> false, redirectUserTo -> testUrl)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessPreconditionFail()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val response = aResponse()
      .withStatus(Status.PRECONDITION_FAILED)
      .withBody(preconditionFailedJson.toString())
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessError()(implicit appConfig: AppConfig, messages: Messages): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val response = aResponse().withStatus(Status.NOT_FOUND)
    stubFor(mapping.willReturn(response))
  }


  val preconditionFailedJson = Json.obj(redirectUserTo -> testUrl)
}
