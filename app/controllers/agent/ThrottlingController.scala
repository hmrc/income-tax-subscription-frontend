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

package controllers.agent

import auth.individual.BaseFrontendController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.{Throttle, ThrottleStartOfJourney}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ThrottlingController @Inject()(val auditingService: AuditingService,
                                     val authService: AuthService,
                                     throttleStart: ThrottleStartOfJourney,
                                     throttle: Throttle)
                                    (implicit mcc: MessagesControllerComponents,
                                     val ec: ExecutionContext,
                                     val appConfig: AppConfig) extends BaseFrontendController {

  def start(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(throttleStart(backLink(), controllers.agent.routes.TaskListController.show())))
  }

  def end(): Action[AnyContent] = Action.async { implicit request => Future.successful(Ok(throttle(backLink()))) }

  private def backLink() = Some(controllers.agent.routes.HomeController.index.url)

}
