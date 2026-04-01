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

package controllers.individual.tasklist.taxyear


import controllers.individual.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.*
import services.mocks.*

class MandatoryBothSignUpControllerSpec extends ControllerBaseSpec
  with MockMandatoryBothSignUp
  with MockAccountingPeriodService
  with MockAuditingService
  with MockSessionDataService {

  override val controllerName: String = "MandatoryBothSignUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestMandatoryBothSignUpController.show,
    "submit" -> TestMandatoryBothSignUpController.submit
  )

  object TestMandatoryBothSignUpController extends MandatoryBothSignUpController(
    mandatoryBothSignUp,
    mockAccountingPeriodService,
    mockSessionDataService
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  "show" should {
    "return OK" in {
      mockView()
      val result = await(TestMandatoryBothSignUpController.show(subscriptionRequest))
      status(result) mustBe Status.OK
    }
  }

  "submit" should {
    "redirect to WhatYouNeedToDo" in {
      val result = await(TestMandatoryBothSignUpController.submit(subscriptionRequest))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
    }
  }
}
