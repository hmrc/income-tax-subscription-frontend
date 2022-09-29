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

package controllers.individual.incomesource

import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockAuditingService
import views.html.individual.incometax.incomesource.CannotUseService

import scala.concurrent.Future

class CannotUseServiceControllerSpec extends ControllerBaseSpec with MockAuditingService {

  override val controllerName: String = "CannotUseServiceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val mockCannotUseServiceView: CannotUseService = mock[CannotUseService]
  when(mockCannotUseServiceView(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestCannotUseServiceController extends CannotUseServiceController(
    mockAuditingService,
    mockAuthService,
    mockCannotUseServiceView
  )

  "Calling the show action of the Cannot Use Service Controller" when {

    def call: Future[Result] = TestCannotUseServiceController.show(subscriptionRequest)

    "return ok (200)" in {
      val result = call
      status(result) must be(Status.OK)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }
}


