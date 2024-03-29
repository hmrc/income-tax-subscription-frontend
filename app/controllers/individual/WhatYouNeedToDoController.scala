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

package controllers.individual

import auth.individual.SignUpController
import common.Constants.ITSASessionKeys
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService}
import views.html.individual.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToDoController @Inject()(whatYouNeedToDo: WhatYouNeedToDo)
                                         (val auditingService: AuditingService,
                                          val appConfig: AppConfig,
                                          val authService: AuthService)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpController {

  val show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      val nextYearOnly = request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")
      val mandatedCurrentYear = request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true")
      val mandatedNextYear = request.session.get(ITSASessionKeys.MANDATED_NEXT_YEAR).contains("true")
      Ok(whatYouNeedToDo(postAction = routes.WhatYouNeedToDoController.submit, nextYearOnly, mandatedCurrentYear, mandatedNextYear))
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(controllers.individual.tasklist.routes.TaskListController.show())
  }

}
