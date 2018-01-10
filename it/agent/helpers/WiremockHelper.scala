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

package agent.helpers

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.{Eventually, IntegrationPatience}

object WiremockHelper extends Eventually with IntegrationPatience {
  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val url = s"http://$wiremockHost:$wiremockPort"

  def verifyPost(uri: String, optBody: Option[String] = None, count: Option[Int] = None): Unit = {
    val countCondition = count match {
      case Some(expectedCount) => exactly(expectedCount)
      case _ => moreThanOrExactly(1)
    }
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(countCondition, postRequest)
  }

  def verifyGet(uri: String, count: Option[Int] = None): Unit = {
    val countCondition = count match {
      case Some(expectedCount) => exactly(expectedCount)
      case _ => moreThanOrExactly(1)
    }
    verify(countCondition, getRequestedFor(urlEqualTo(uri)))
  }

  def verifyDelete(uri: String, count: Option[Int] = None): Unit = {
    val countCondition = count match {
      case Some(expectedCount) => exactly(expectedCount)
      case _ => moreThanOrExactly(1)
    }
    verify(countCondition, deleteRequestedFor(urlEqualTo(uri)))
  }

  def verifyPut(uri: String, optBody: Option[String] = None, count: Option[Int] = None): Unit = {
    val countCondition = count match {
      case Some(expectedCount) => exactly(expectedCount)
      case _ => moreThanOrExactly(1)
    }
    val uriMapping = putRequestedFor(urlEqualTo(uri))
    val putRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(countCondition, putRequest)
  }

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPut(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubDelete(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
}

