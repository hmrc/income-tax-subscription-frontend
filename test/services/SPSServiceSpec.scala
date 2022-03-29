/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.SPSConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import utilities.UnitTestTrait

class SPSServiceSpec extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  "Augment the mtditid with prefix" in {
    val mockConnector = mock[SPSConnector]
    val service = new SPSService(mockConnector)
    service.confirmPreferences("mtdItsaId", Some("spsEntityId"))

    verify(mockConnector).postSpsConfirm(ArgumentMatchers.eq("spsEntityId"), ArgumentMatchers.eq("HMRC-MTD-IT~MTDITID~mtdItsaId"))(any())
  }

  "absent enrolment key" should {
    "Not interact with the sps connector" in {
      val mockConnector = mock[SPSConnector]
      val service = new SPSService(mockConnector)

      service.confirmPreferences("mtdItsaId", None)

      verify(mockConnector, times(0)).postSpsConfirm(any(), any())(any())
    }
  }
}
