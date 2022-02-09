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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.individual.Timeout

class SessionTimeoutControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "SessionTimeoutController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config:Configuration = app.injector.instanceOf[Configuration]

  private val sessionTimeoutView = mock[Timeout]

  when(sessionTimeoutView()(any(), any(), any()))
    .thenReturn(HtmlFormat.empty)

  object TestSessionTimeoutController extends SessionTimeoutController(sessionTimeoutView, mockMessagesControllerComponents, config, env)(appConfig)

  "Calling the timeout action of the SessionTimeoutController" should {

    lazy val result = TestSessionTimeoutController.show(subscriptionRequest)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }
}
