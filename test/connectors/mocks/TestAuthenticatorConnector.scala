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
import connectors.models.matching.{UserMatchFailureResponseModel, UserMatchRequestModel, UserMatchSuccessResponseModel, UserMatchUnexpectedError}
import models.matching.UserDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.JsonUtils._
import utils.TestConstants.testException
import utils.TestModels._
import utils.{MockTrait, TestConstants, UnitTestTrait}

import scala.concurrent.Future

trait TestAuthenticatorConnector extends UnitTestTrait with MockHttp {

  object TestAuthenticatorConnector extends AuthenticatorConnector(
    appConfig, mockHttpPost, app.injector.instanceOf[Logging])

  def setupMockMatchUser(userDetailsModel: Option[UserDetailsModel])(status: Int, response: JsValue): Unit =
    setupMockHttpPost(TestAuthenticatorConnector.matchingEndpoint,
      userDetailsModel map UserMatchRequestModel.apply)(status, response)

  // use this function if we want to match on the UserDetailsModel used in the parameter
  val setupMockMatchUser: UserDetailsModel => ((Int, JsValue)) => Unit =
    (userDetailsModel: UserDetailsModel) => (setupMockMatchUser(None) _).tupled

  // use this function if we don't care about what UserDetailsModel is used in the parameter
  val setupMatchUser: ((Int, JsValue)) => Unit =
    (setupMockMatchUser(None) _).tupled

  def matchUserMatched(nino: String = TestConstants.testNino): (Int, JsValue) = (OK,
    UserMatchSuccessResponseModel.format.writes(testMatchSuccessModel.copy(nino = nino)))

  val matchUserNoMatch: (Int, JsValue) = (UNAUTHORIZED,
    """{
      | "errors" : "CID returned no record"
      |}""".stripMargin: JsValue)

  val matchUserUnexpectedFailure: (Int, JsValue) = (UNAUTHORIZED,
    """{
      | "errors" : "Internal error: unexpected result from matching"
      |}""".stripMargin: JsValue)

  val matchUserUnexpectedStatus: (Int, JsValue) = (NOT_FOUND,
    """{}""".stripMargin: JsValue)
}

trait MockAuthenticatiorConnector extends MockTrait {

  val mockAuthenticatiorConnector = mock[AuthenticatorConnector]


  private def mockUserMatch(userDetails: UserDetailsModel)
                           (response: Future[Either[UserMatchFailureResponseModel, Option[UserMatchSuccessResponseModel]]]): Unit =
    when(
      mockAuthenticatiorConnector.matchUser(
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

  def mockUserMatchNotFound(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(None))
  }

  def mockUserMatchFailure(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Left(UserMatchUnexpectedError)))
  }

  def mockUserMatchException(userDetails: UserDetailsModel): Unit =
    mockUserMatch(userDetails)(Future.failed(testException))
}