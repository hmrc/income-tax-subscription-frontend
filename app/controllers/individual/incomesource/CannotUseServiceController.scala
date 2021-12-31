/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.incomesource

import auth.individual.StatelessController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.individual.incometax.incomesource.CannotUseService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CannotUseServiceController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val cannotUseServiceView: CannotUseService)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends StatelessController {

  val show: Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      Future.successful(Ok(cannotUseServiceView(
        postAction = controllers.individual.incomesource.routes.CannotUseServiceController.show
      )))
  }

}
