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

package controllers.agent.handoffs

import config.AppConfig
import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.*
import views.html.agent.handoffs.CheckClientIncomeSources

class CheckClientIncomeSourcesControllerSpec extends AgentControllerBaseSpec with MockIdentifierAction {

  override val controllerName: String = "CheckClientIncomeSourcesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config: Configuration = app.injector.instanceOf[Configuration]

  class TestController extends CheckClientIncomeSourcesController(
    app.injector.instanceOf[CheckClientIncomeSources],
    fakeIdentifierAction,
    appConfig
  )

  "show returns Ok" in {
    val result = new TestController().show()(fakeRequest)
    status(result) must be(Status.OK)
    contentType(result) must be(Some("text/html"))
    charset(result) must be(Some("utf-8"))
  }
}