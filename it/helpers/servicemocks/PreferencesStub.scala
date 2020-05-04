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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor}
import config.AppConfig
import connectors.PaperlessPreferenceHttpParser._
import utilities.individual.Constants._
import helpers.IntegrationTestConstants._
import play.api.http.Status
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}


object PreferencesStub extends WireMockMethods {

  val newPreferencesUrl = s".*/paperless/activate-from-token/$preferencesServiceKey.*"

  def stubPaperlessActivated(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> true)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessInactive(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> false)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessInactiveWithUri(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val rBody = Json.obj("optedIn" -> false, redirectUserTo -> testUrl)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessPreconditionFail(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse()
      .withStatus(Status.PRECONDITION_FAILED)
      .withBody(preconditionFailedJson.toString())
    stubFor(mapping.willReturn(response))
  }

  def stubPaperlessError(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(".*/paperless/activate.*"))
    val response = aResponse().withStatus(Status.NOT_FOUND)
    stubFor(mapping.willReturn(response))
  }

//  Feature Switch methods for new Preferences sign-up solution

  def newStubPaperlessActivated(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val rBody = Json.obj("optedIn" -> true)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessInactiveWithUri(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val rBody = Json.obj("optedIn" -> false, redirectUserTo -> testUrl)
    val response = aResponse().withStatus(Status.OK).withBody(rBody.toString)
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessPreconditionFail(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val response = aResponse()
      .withStatus(Status.PRECONDITION_FAILED)
      .withBody(preconditionFailedJson.toString())
    stubFor(mapping.willReturn(response))
  }

  def newStubPaperlessError(): Unit = {
    val mapping = PUT.wireMockMapping(WireMock.urlPathMatching(newPreferencesUrl))
    val response = aResponse().withStatus(Status.NOT_FOUND)
    stubFor(mapping.willReturn(response))
  }


  val preconditionFailedJson: JsObject = Json.obj(redirectUserTo -> testUrl)
}
