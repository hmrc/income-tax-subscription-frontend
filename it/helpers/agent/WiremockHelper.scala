package helpers.agent

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
