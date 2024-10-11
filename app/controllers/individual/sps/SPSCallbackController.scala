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

package controllers.individual.sps

import auth.individual.StatelessController
import common.Constants.ITSASessionKeys
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SPSCallbackController @Inject()(val auditingService: AuditingService,
                                      val authService: AuthService)
                                     (implicit val appConfig: AppConfig,
                                      val ec: ExecutionContext,
                                      mcc: MessagesControllerComponents) extends StatelessController {

  def callback(entityId: Option[String]): Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      entityId match {
        case Some(entityId) =>
          val result = Redirect(controllers.individual.routes.UsingSoftwareController.show())
          result.addingToSession(
            ITSASessionKeys.SPSEntityId -> entityId
          )
        case None => throw new InternalServerException("[SPSCallbackController][callback] - Entity Id was not found")
      }
  }

}
