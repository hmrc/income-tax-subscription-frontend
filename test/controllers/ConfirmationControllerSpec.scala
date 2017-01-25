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

package controllers

import play.api.mvc.{Action, AnyContent}

class ConfirmationControllerSpec extends ControllerBaseSpec {

  object TestConfirmationController extends ConfirmationController(
    MockBaseControllerConfig,
    messagesApi
  )

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationController.showConfirmation
  )

  //  "The Summary controller" should {
  //    "use the correct applicationConfig" in {
  //      ConfirmationController.applicationConfig must be(FrontendAppConfig)
  //    }
  //    "use the correct authConnector" in {
  //      ConfirmationController.authConnector must be(FrontendAuthConnector)
  //    }
  //    "use the correct postSignInRedirectUrl" in {
  //      ConfirmationController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
  //    }
  //  }

  authorisationTests

}
