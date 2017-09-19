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

package services.mocks

import connectors.mocks.MockPreferenceFrontendConnector
import connectors.models.preferences._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Request}
import services.PreferencesService
import utils.TestConstants._
import utils.{MockTrait, UnitTestTrait}

import scala.concurrent.Future

trait MockPreferencesService extends MockTrait {
  val mockPreferencesService = mock[PreferencesService]

  private def mockCheckPaperless(token: String)(result: Future[Either[PaperlessPreferenceError.type, PaperlessState]]): Unit =
    when(mockPreferencesService.checkPaperless(ArgumentMatchers.eq(token))(ArgumentMatchers.any[Request[AnyContent]]))
      .thenReturn(result)

  def mockCheckPaperlessActivated(token: String): Unit = mockCheckPaperless(token)(Future.successful(Right(Activated)))

  def mockCheckPaperlessUnset(token: String, url: Option[String]): Unit = mockCheckPaperless(token)(Future.successful(Right(Unset(url))))

  def mockCheckPaperlessException(token: String): Unit = mockCheckPaperless(token)(Future.failed(testException))

  def mockChoosePaperlessUrl(url: String): Unit =
    when(mockPreferencesService.defaultChoosePaperlessUrl(ArgumentMatchers.any[Request[AnyContent]]))
      .thenReturn(url)

}

trait TestPreferencesService extends UnitTestTrait with MockPreferenceFrontendConnector {

  object TestPreferencesService extends PreferencesService(mockPreferenceFrontendConnector)

}
