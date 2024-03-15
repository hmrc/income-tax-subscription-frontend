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

package connectors.mocks

import connectors.SessionDataConnector
import connectors.httpparser.GetSessionDataHttpParser.GetSessionDataFailure
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait MockSessionDataConnector extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockSessionDataConnector: SessionDataConnector = mock[SessionDataConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionDataConnector)
  }

  def mockGetSessionData[T](id: String)
                           (result: Either[GetSessionDataFailure, Option[T]]): Unit = {
    when(mockSessionDataConnector.getSessionData[T](ArgumentMatchers.eq(id))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }


}
