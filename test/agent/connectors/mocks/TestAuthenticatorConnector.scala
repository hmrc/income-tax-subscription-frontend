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

package agent.connectors.mocks

import agent.audit.Logging
import agent.connectors.matching.AuthenticatorConnector
import agent.connectors.models.matching._
import agent.models.agent.ClientDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.JsonUtils._
import agent.utils.TestConstants._
import agent.utils.{MockTrait, TestConstants, UnitTestTrait}

import scala.concurrent.Future

trait TestAuthenticatorConnector extends UnitTestTrait with MockHttp {

  object TestAuthenticatorConnector extends AuthenticatorConnector(
    appConfig, mockHttpPost, app.injector.instanceOf[Logging])

  def setupMockMatchClient(clientDetailsModel: Option[ClientDetailsModel])(status: Int, response: JsValue): Unit =
    setupMockHttpPost(TestAuthenticatorConnector.matchingEndpoint,
      clientDetailsModel.fold(None: Option[ClientMatchRequestModel])(x => x: ClientMatchRequestModel))(status, response)

  // use this function if we want to match on the ClientDetailsModel used in the parameter
  val setupMockMatchClient: ClientDetailsModel => ((Int, JsValue)) => Unit =
    (clientDetailsModel: ClientDetailsModel) => (setupMockMatchClient(None) _).tupled

  // use this function if we don't care about what ClientDetailsModel is used in the parameter
  val setupMatchClient: ((Int, JsValue)) => Unit =
    (setupMockMatchClient(None) _).tupled

  def matchClientMatched(nino: String = TestConstants.testNino): (Int, JsValue) = (OK,
    s"""{
       | "firstName" : "",
       | "lastName" : "",
       | "dateOfBirth" : "",
       | "postCode" : "",
       | "nino" : "$nino",
       | "saUtr" : ""
       |}""".stripMargin: JsValue)

  val matchClientNoMatch: (Int, JsValue) = (UNAUTHORIZED,
    """{
      | "errors" : "CID returned no record"
      |}""".stripMargin: JsValue)

  val matchClientUnexpectedFailure: (Int, JsValue) = (UNAUTHORIZED,
    """{
      | "errors" : "Internal error: unexpected result from matching"
      |}""".stripMargin: JsValue)

  val matchClientUnexpectedStatus: (Int, JsValue) = (NOT_FOUND,
    """{}""".stripMargin: JsValue)
}

trait MockAuthenticatorConnector extends MockTrait {

  val mockAuthenticatorConnector = mock[AuthenticatorConnector]

  private def mockClientMatch(clientDetails: ClientDetailsModel)
                           (response: Future[Option[String]]): Unit =
    when(
      mockAuthenticatorConnector.matchClient(
        ArgumentMatchers.eq(clientDetails)
      )(
        ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(response)

  def mockClientMatchSuccess(clientDetails: ClientDetailsModel): Unit = {
    mockClientMatch(clientDetails)(Future.successful(Some(testNino)))
  }

  def mockClientMatchNotFound(clientDetails: ClientDetailsModel): Unit = {
    mockClientMatch(clientDetails)(Future.successful(None))
  }

  def mockClientMatchException(clientDetails: ClientDetailsModel): Unit =
    mockClientMatch(clientDetails)(Future.failed(testException))
}
