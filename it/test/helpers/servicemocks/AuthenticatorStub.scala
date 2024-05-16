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

import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels
import models.usermatching.{UserMatchFailureResponseModel, UserMatchRequestModel, UserMatchSuccessResponseModel}
import play.api.http.Status

object AuthenticatorStub extends WireMockMethods {
  def stubMatchFound(returnedNino: String, returnedUtr: Option[String] = Some(testUtr)): Unit = {
    val model = UserMatchRequestModel.apply(IntegrationTestModels.testUserDetails)

    val returnMessage = UserMatchSuccessResponseModel("", "", "", nino = returnedNino, returnedUtr)
    when(method = POST, uri = "/authenticator/match", body = model)
      .thenReturn(status = Status.OK, returnMessage)
  }

  def stubMatchNotFound(): Unit = {
    val model = UserMatchRequestModel.apply(IntegrationTestModels.testUserDetails)

    val returnMessage = UserMatchFailureResponseModel("")

    when(method = POST, uri = "/authenticator/match", body = model)
      .thenReturn(status = Status.UNAUTHORIZED, returnMessage)
  }

  def stubMatchDeceased(): Unit = {
    val model = UserMatchRequestModel.apply(IntegrationTestModels.testUserDetails)

    when(method = POST, uri = "/authenticator/match", body = model)
      .thenReturn(status = Status.FAILED_DEPENDENCY, "")
  }

  def stubMatchFailure(): Unit = {
    val model = UserMatchRequestModel.apply(IntegrationTestModels.testUserDetails)

    val returnMessage = UserMatchFailureResponseModel("")

    when(method = POST, uri = "/authenticator/match", body = model)
      .thenReturn(status = Status.INTERNAL_SERVER_ERROR, returnMessage)
  }
}