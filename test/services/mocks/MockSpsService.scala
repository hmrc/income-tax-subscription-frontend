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

package services.mocks

import connectors.SPSConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.SPSService
import utilities.UnitTestTrait

trait MockSpsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockSpsService: SPSService = mock[SPSService]
  val mockSpsConnector = mock[SPSConnector]
  val testEntityId = "testEntityId"
  val testMtditid = "testMtditid"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSpsService)
  }

  def verifyConfirmPreferencesPostSpsConfirm(testEntityId: String, testMtditid: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(mockSpsService, times(count)).confirmPreferences(ArgumentMatchers.eq(testEntityId), ArgumentMatchers.eq(testMtditid))(
      ArgumentMatchers.any()))

}
