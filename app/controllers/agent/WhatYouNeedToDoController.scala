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

package controllers.agent

import auth.agent.AuthenticatedController
import common.Constants.ITSASessionKeys
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService}
import views.html.agent.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToDoController @Inject()(whatYouNeedToDo: WhatYouNeedToDo)
                                         (val auditingService: AuditingService,
                                          val appConfig: AppConfig,
                                          val authService: AuthService)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends AuthenticatedController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      val eligibleNextYearOnly: Boolean = request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")
      val mandatedCurrentYear: Boolean = request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true")
      val mandatedNextYear: Boolean = request.session.get(ITSASessionKeys.MANDATED_NEXT_YEAR).contains("true")
      Ok(whatYouNeedToDo(
        postAction = routes.WhatYouNeedToDoController.submit,
        eligibleNextYearOnly = eligibleNextYearOnly,
        mandatedCurrentYear = mandatedCurrentYear,
        mandatedNextYear = mandatedNextYear
      ))
  }

  def submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(controllers.agent.routes.TaskListController.show())
  }

}
