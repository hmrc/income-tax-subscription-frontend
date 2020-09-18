/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.business

import java.util.UUID

import config.AppConfig
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class InitialiseController @Inject()(mcc: MessagesControllerComponents, authService: AuthService)
                                    (implicit val ec: ExecutionContext, val appConfig: AppConfig) extends FrontendController(mcc) {


  val initialise: Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      val id = UUID.randomUUID().toString

      Future.successful(Redirect(controllers.individual.business.routes.BusinessStartDateController.show(id)))
    }
  }
}
