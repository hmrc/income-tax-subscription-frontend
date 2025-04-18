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

package services.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import services.SessionClearingService

import scala.concurrent.Future

trait MockSessionClearingService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockSessionClearingService: SessionClearingService = mock[SessionClearingService]

  override def beforeEach(): Unit = {
    reset(mockSessionClearingService)
    super.beforeEach()
  }

  def mockClearAgentSessionSuccess(result: Result): Unit = {
    when(mockSessionClearingService.clearAgentSession(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockClearAgentSessionFailure(failure: Throwable): Unit = {
    when(mockSessionClearingService.clearAgentSession(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenThrow(failure)
  }

}
