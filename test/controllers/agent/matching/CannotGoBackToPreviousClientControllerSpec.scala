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

package controllers.agent.matching

import config.MockConfig
import controllers.ControllerSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import views.agent.mocks.MockCannotGoBackToPreviousClient

import scala.concurrent.Future

class CannotGoBackToPreviousClientControllerSpec extends ControllerSpec
  with MockCannotGoBackToPreviousClient
  with MockIdentifierAction {

  object TestCannotGoBackToPreviousClient extends CannotGoBackToPreviousClientController(
    mockCannotGoBackToPreviousClient,
    fakeIdentifierAction
  )(cc, MockConfig)

  "show" must {
    "return OK with the page content" in {
      mockView()

      val result: Future[Result] = TestCannotGoBackToPreviousClient.show()(request)

      status(result) must be(Status.OK)
      contentType(result) mustBe Some(HTML)
    }
  }
}
