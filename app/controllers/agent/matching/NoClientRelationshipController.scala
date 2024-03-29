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

package controllers.agent.matching

import auth.agent.UserMatchingController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.agent.matching.NoClientRelationship

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NoClientRelationshipController @Inject()(val auditingService: AuditingService,
                                               val authService: AuthService,
                                               noClientRelationship: NoClientRelationship)
                                              (implicit val ec: ExecutionContext,
                                               mcc: MessagesControllerComponents,
                                               val appConfig: AppConfig) extends UserMatchingController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      Future.successful(Ok(noClientRelationship(postAction = controllers.agent.matching.routes.NoClientRelationshipController.submit)))
  }

  val submit: Action[AnyContent] = Authenticated.async { _ =>
    _ =>
      Future.successful(Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
  }
}
