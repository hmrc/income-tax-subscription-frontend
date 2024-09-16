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

import auth.agent.PreSignUpController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService}
import views.html.agent.matching.NoClientRelationship

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NoClientRelationshipController @Inject()(val auditingService: AuditingService,
                                               val authService: AuthService,
                                               clientDetailsRetrieval: ClientDetailsRetrieval,
                                               noClientRelationship: NoClientRelationship)
                                              (implicit val ec: ExecutionContext,
                                               mcc: MessagesControllerComponents,
                                               val appConfig: AppConfig) extends PreSignUpController {

  def view(clientName: String, clientNino: String)(implicit request: Request[_]): Html = {
    noClientRelationship(
      postAction = controllers.agent.matching.routes.NoClientRelationshipController.submit,
      clientName,
      clientNino
    )
  }

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      clientDetailsRetrieval.getClientDetails map { clientDetails =>
        Ok(view(clientDetails.name, clientDetails.formattedNino))
      }
  }

  val submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
  }
}
