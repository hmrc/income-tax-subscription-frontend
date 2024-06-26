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

package connectors.usermatching.mocks

import auth.MockHttp
import connectors.usermatching.AuthenticatorConnector
import models.usermatching._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.TestModels._
import utilities.UnitTestTrait
import utilities.individual.TestConstants
import utilities.individual.TestConstants.testException

import scala.concurrent.Future

trait TestAuthenticatorConnector extends UnitTestTrait with MockHttp {

  object TestAuthenticatorConnector extends AuthenticatorConnector(appConfig, mockHttp)

  def setupMockMatchUser(userDetailsModel: Option[UserDetailsModel])(status: Int, response: JsValue): Unit =
    setupMockHttpPost(Some(TestAuthenticatorConnector.matchingEndpoint),
      userDetailsModel map UserMatchRequestModel.apply)(status, response)

  // use this function if we want to match on the UserDetailsModel used in the parameter
  val setupMockMatchUser: UserDetailsModel => ((Int, JsValue)) => Unit =
    (userDetailsModel: UserDetailsModel) => (setupMockMatchUser(None) _).tupled

  // use this function if we don't care about what UserDetailsModel is used in the parameter
  val setupMatchUser: ((Int, JsValue)) => Unit =
    (setupMockMatchUser(None) _).tupled

  def matchUserMatched(nino: String = TestConstants.testNino): (Int, JsValue) = (OK,
    UserMatchSuccessResponseModel.format.writes(testMatchSuccessModel.copy(nino = nino)))

  val matchUserNoMatch: (Int, JsValue) = (UNAUTHORIZED, Json.obj("errors" -> "CID returned no record"))

  val matchUserUnexpectedFailure: (Int, JsValue) = (UNAUTHORIZED, Json.obj("errors" -> "Internal error: unexpected result from matching"))

  val matchUserUnexpectedStatus: (Int, JsValue) = (NOT_FOUND, Json.obj())
}

trait MockAuthenticatiorConnector extends UnitTestTrait with MockitoSugar {

  val mockAuthenticatiorConnector: AuthenticatorConnector = mock[AuthenticatorConnector]

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
    mockUserMatch(userDetails)(Future.successful(Right(None)))
  }

  def mockUserMatchException(userDetails: UserDetailsModel): Unit =
    mockUserMatch(userDetails)(Future.failed(testException))
}
