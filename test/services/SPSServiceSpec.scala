/*
 * Copyright 2021 HM Revenue & Customs
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

package services;

import config.featureswitch.FeatureSwitch.SPSEnabled
import connectors.SPSConnector
import org.mockito.ArgumentMatchers.{any, startsWith}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Session}
import utilities.ITSASessionKeys.{SPSEntityId, mtdItsaEnrolmentIdentifierKey}
import utilities.UnitTestTrait

class SPSServiceSpec extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  "Sps subscription service with enabled feature switch" should {
    "Augment the mtditid with prefix" in {
      val mockConnector = mock[SPSConnector]
      val service = new SPSService(mockConnector)
      service.enable(SPSEnabled)
      val mockRequest = mock[Request[AnyContent]]
      val mockSession = mock[Session]
      when(mockSession.get(SPSEntityId)).thenReturn("something")
      when(mockSession.get(mtdItsaEnrolmentIdentifierKey)).thenReturn("something else")
      when(mockRequest.session).thenReturn(mockSession)
      service.confirmPreferences(mockRequest)

      verify(mockConnector).postSpsConfirm(any(), startsWith("HMRC-MTD-IT~MTDITID~"))(any())
    }
  }

  "Sps subscription service with disabled feature switch" should {
    "Not interact with the sps connector" in {
      val mockConnector = mock[SPSConnector]
      val service = new SPSService(mockConnector)
      service.disable(SPSEnabled)
      val mockRequest = mock[Request[AnyContent]]
      service.confirmPreferences(mockRequest)

      verify(mockConnector, times(0)).postSpsConfirm(any(), any())(any())
    }
  }

  "Sps subscription service with enabled feature switch but absent enrolment key" should {
    "Not interact with the sps connector" in {
      val mockConnector = mock[SPSConnector]
      val service = new SPSService(mockConnector)
      service.enable(SPSEnabled)
      val mockRequest = mock[Request[AnyContent]]
      val mockSession = mock[Session]
      when(mockSession.get(SPSEntityId)).thenReturn("something")
      when(mockSession.get(mtdItsaEnrolmentIdentifierKey)).thenReturn(Option.empty)

      when(mockRequest.session).thenReturn(mockSession)
      service.confirmPreferences(mockRequest)

      verify(mockConnector, times(0)).postSpsConfirm(any(), any())(any())
    }
  }
}
