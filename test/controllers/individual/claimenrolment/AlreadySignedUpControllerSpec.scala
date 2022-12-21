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

import controllers.individual.ControllerBaseSpec
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers._
import services.mocks.MockAuditingService
import views.individual.mocks.MockAlreadySignedUp

import scala.concurrent.Future

class AlreadySignedUpControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  
  with MockAlreadySignedUp {

  override val controllerName: String = "ClaimEnrolmentAlreadySignedUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAlreadySignedUpController.show
  )

  object TestAlreadySignedUpController extends ClaimEnrolmentAlreadySignedUpController(
    mockAuthService,
    mockAuditingService,
    alreadySignedUp
  )

  "show" should {
    "return an OK status with the already signed up page" in {
        mockAlreadySignedUp()
        val result: Future[Result] = TestAlreadySignedUpController.show()(claimEnrolmentRequest)
        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }
}
