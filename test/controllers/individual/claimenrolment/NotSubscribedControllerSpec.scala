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

package controllers.individual.claimenrolment

import controllers.ControllerBaseSpec
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers._
import services.mocks.MockAuditingService
import views.individual.mocks.MockNotSubscribed

import scala.concurrent.Future

class NotSubscribedControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  
  with MockNotSubscribed {

  override val controllerName: String = "NotSubscribedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestNotSubscribedController.show
  )

  object TestNotSubscribedController extends NotSubscribedController(
    mockAuditingService,
    mockAuthService,
    notSubscribed
  )

  "show" should {
    "return an OK status with the not subscribed page" in {
        mockNotSubscribed()

        val result: Future[Result] = TestNotSubscribedController.show()(claimEnrolmentRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

}
