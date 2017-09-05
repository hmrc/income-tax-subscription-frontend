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

package connectors.mocks

import audit.Logging
import connectors.matching.AuthenticatorConnector
import connectors.models.matching.{UserMatchFailureResponseModel, UserMatchRequestModel, UserMatchSuccessResponseModel}
import models.matching.UserDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.JsonUtils._
import utils.TestConstants.testException
import utils.TestModels._
import utils.{MockTrait, TestConstants, UnitTestTrait}

import scala.concurrent.Future

trait TestAuthenticationConnector extends UnitTestTrait with MockHttp {

  object TestAuthenticatorConnector extends AuthenticatorConnector(
    appConfig, mockHttpPost, app.injector.instanceOf[Logging])

  def setupMockMatchClient(clientDetailsModel: Option[UserDetailsModel])(status: Int, response: JsValue): Unit =
    setupMockHttpPost(TestAuthenticatorConnector.matchingEndpoint,
      clientDetailsModel.fold(None: Option[UserMatchRequestModel])(x => x: UserMatchRequestModel))(status, response)

  // use this function if we want to match on the ClientDetailsModel used in the parameter
  val setupMockMatchClient: UserDetailsModel => ((Int, JsValue)) => Unit =
    (userDetailsModel: UserDetailsModel) => (setupMockMatchClient(None) _).tupled

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

trait MockAuthenticationConnector extends MockTrait {

  val mockAuthenticationConnector = mock[AuthenticatorConnector]


  private def mockUserMatch(userDetails: UserDetailsModel)
                           (response: Future[Either[UserMatchFailureResponseModel, Option[UserMatchSuccessResponseModel]]]): Unit =
    when(
      mockAuthenticationConnector.matchClient(
        ArgumentMatchers.eq(userDetails)
      )(
        ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(response)

  def mockUserMatchSuccess(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Right(Some(testMatchSuccessModel))))
  }
  def mockUserMatchNoUtr(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Right(Some(testMatchNoUtrModel))))
  }
  def mockUserMatchFailure(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(None))
  }
  def mockUserMatchException(userDetails: UserDetailsModel): Unit =
    mockUserMatch(userDetails)(Future.failed(testException))
}