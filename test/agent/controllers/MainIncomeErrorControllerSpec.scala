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

package agent.controllers

import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class MainIncomeErrorControllerSpec extends AgentControllerBaseSpec {

  override val controllerName: String = "MainIncomeErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "mainIncomeError" -> TestMainIncomeErrorController.show
  )

  object TestMainIncomeErrorController extends MainIncomeErrorController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService
  )

  "Calling the mainIncomeError action of the MainIncomeErrorController" should {

    lazy val result = TestMainIncomeErrorController.show(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }

  "The back url" should {
    s"point to ${agent.controllers.routes.IncomeSourceController.show().url}" in {
      TestMainIncomeErrorController.backUrl mustBe agent.controllers.routes.IncomeSourceController.show().url
    }
  }

  authorisationTests()

}
