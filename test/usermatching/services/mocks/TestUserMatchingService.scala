/*
 * Copyright 2020 HM Revenue & Customs
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

package usermatching.services.mocks

import connectors.usermatching.mocks.MockAuthenticatiorConnector
import core.utils.MockTrait
import core.utils.TestConstants._
import core.utils.TestModels._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.models.{UserDetailsModel, UserMatchFailureResponseModel, UserMatchSuccessResponseModel}
import usermatching.services.UserMatchingService
import usermatching.utils.UserMatchingTestSupport

import scala.concurrent.Future

trait TestUserMatchingService extends MockAuthenticatiorConnector {

  object TestUserMatchingService extends UserMatchingService(appConfig, mockAuthenticatiorConnector)

}

trait MockUserMatchingService extends MockTrait with UserMatchingTestSupport {
  val mockUserMatchingService = mock[UserMatchingService]

  private def mockUserMatch(userDetails: UserDetailsModel)
                           (response: Future[Either[UserMatchFailureResponseModel, Option[UserMatchSuccessResponseModel]]]): Unit =
    when(
      mockUserMatchingService.matchUser(
        ArgumentMatchers.eq(userDetails)
      )(
        ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(response)

  def mockUserMatchSuccess(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Right(Some(testMatchSuccessModel))))
  }

  def mockUserMatchSuccessNoUtr(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Right(Some(testMatchSuccessModel.copy(saUtr = None)))))
  }

  def mockUserMatchNotFound(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(None))
  }

  def mockUserMatchNoUtr(userDetails: UserDetailsModel): Unit = {
    mockUserMatch(userDetails)(Future.successful(Right(Some(testMatchNoUtrModel))))
  }

  def mockUserMatchException(userDetails: UserDetailsModel): Unit =
    mockUserMatch(userDetails)(Future.failed(testException))
}
