/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent.matching

import common.Constants.ITSASessionKeys
import config.AppConfig
import connectors.agent.mocks.MockIncomeTaxSessionDataConnector
import controllers.ControllerSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import models.SessionData
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.libs.json.JsString
import play.api.mvc.*
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.InternalServerException

class ClientVAndCHomeControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockIncomeTaxSessionDataConnector {

  private def testController(sessionData: SessionData = SessionData()) = new ClientVAndCHomeController(
    fakeIdentifierActionWithSessionData(sessionData),
    mockIncomeTaxSessionDataConnector,
    appConfig
  )

  val fullSessionData: SessionData = SessionData(Map(
    ITSASessionKeys.MTDITID -> JsString("test-mtditid"),
    ITSASessionKeys.NINO -> JsString("test-nino"),
    ITSASessionKeys.UTR -> JsString("test-utr")
  ))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeTaxSessionDataConnector)
    when(appConfig.getVAndCUrl).thenReturn("http://localhost:9081/report-quarterly/income-and-expenses/view")
  }

  val appConfig: AppConfig = mock[AppConfig]

  "handOffVandC" when {

    "all session data is present and the connector returns true" should {
      "redirect to the view and change" in {
        mockSetupVAndCSessionData("test-mtditid", "test-nino", "test-utr")(true)

        val result = testController(fullSessionData).handOffVAndC()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.getVAndCUrl)
      }
    }

    "all session data is present but the connector returns false" should {
      "throw an InternalServerException" in {
        mockSetupVAndCSessionData("test-mtditid", "test-nino", "test-utr")(false)

        intercept[InternalServerException] {
          await(testController(fullSessionData).handOffVAndC()(request))
        }.message mustBe "[ClientVAndCHomeController][show] - failed to set up view and change session data"
      }
    }

    "mtditid is missing from session data" should {
      "throw an InternalServerException without calling the connector" in {
        val result = testController(SessionData(Map(
          ITSASessionKeys.NINO -> JsString("test-nino"),
          ITSASessionKeys.UTR -> JsString("test-utr")
        ))).handOffVAndC()(request)

        intercept[InternalServerException] {
          await(result)
        }.message mustBe "[ClientVAndCHomeController][show] - missing required session data for view and change handoff"

        verifyNoInteractions(mockIncomeTaxSessionDataConnector)
      }
    }

    "nino is missing from session data" should {
      "throw an InternalServerException without calling the connector" in {
        val result = testController(SessionData(Map(
          ITSASessionKeys.MTDITID -> JsString("test-mtditid"),
          ITSASessionKeys.UTR -> JsString("test-utr")
        ))).handOffVAndC()(request)

        intercept[InternalServerException] {
          await(result)
        }.message mustBe "[ClientVAndCHomeController][show] - missing required session data for view and change handoff"

        verifyNoInteractions(mockIncomeTaxSessionDataConnector)
      }
    }

    "utr is missing from session data" should {
      "throw an InternalServerException without calling the connector" in {
        val result = testController(SessionData(Map(
          ITSASessionKeys.MTDITID -> JsString("test-mtditid"),
          ITSASessionKeys.NINO -> JsString("test-nino")
        ))).handOffVAndC()(request)

        intercept[InternalServerException] {
          await(result)
        }.message mustBe "[ClientVAndCHomeController][show] - missing required session data for view and change handoff"

        verifyNoInteractions(mockIncomeTaxSessionDataConnector)
      }
    }

    "session data is completely empty" should {
      "throw an InternalServerException without calling the connector" in {
        intercept[InternalServerException] {
          await(testController().handOffVAndC()(request))
        }.message mustBe "[ClientVAndCHomeController][show] - missing required session data for view and change handoff"

        verifyNoInteractions(mockIncomeTaxSessionDataConnector)
      }
    }
  }
}
