/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent

import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitching
import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{EligibilityStatus, No, SessionData, Yes}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockGetEligibilityStatusService, MockMandationStatusService, MockSessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockUsingSoftware

import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerSpec
  with MockUsingSoftware
  with MockIdentifierAction
  with MockSessionDataService
  with FeatureSwitching {

  "show" must {
    "return OK with the page content" in {
      mockView(
        postAction = routes.UsingSoftwareController.submit(),
        backUrl = appConfig.govukGuidanceITSASignUpAgentLink
      )

      val result: Future[Result] = testUsingSoftwareController().show()(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" when {
    "the user presses the continue button" should {
      "redirect to the client details page" in {
        val result: Future[Result] =
          testUsingSoftwareController().submit()(request.withMethod("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }
  }

  val appConfig: AppConfig = mock[AppConfig]

  private def testUsingSoftwareController(sessionData: SessionData = SessionData()) = new UsingSoftwareController(
    mockUsingSoftware,
    fakeIdentifierActionWithSessionData(sessionData)
  )(appConfig)
}

