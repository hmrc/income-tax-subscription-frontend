/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual.tasklist.addbusiness

import auth.individual.StatelessController
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService}
import views.html.individual.tasklist.addbusiness.BusinessAlreadyRemoved

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BusinessAlreadyRemovedController @Inject()(businessAlreadyRemoved: BusinessAlreadyRemoved)
                                                (val auditingService: AuditingService,
                                                 val appConfig: AppConfig,
                                                 val authService: AuthService)
                                                (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends StatelessController {

  def show(): Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(businessAlreadyRemoved())
  }
}
