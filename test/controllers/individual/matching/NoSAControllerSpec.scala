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

package controllers.individual.matching

import controllers.individual.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import views.individual.mocks.MockNoSA

class NoSAControllerSpec extends ControllerBaseSpec with MockNoSA {

  override val controllerName: String = "NoSAController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestNoSAController extends NoSAController(noSA, mockMessagesControllerComponents)

  "Calling the show action of the NoSAController" should {

    "return an OK result with HTML" in {
      mockNoSA()

      val result = TestNoSAController.show(subscriptionRequest)

      status(result) must be(Status.OK)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }

}
