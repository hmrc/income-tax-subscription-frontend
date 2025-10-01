/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.agent.tasklist.addbusiness

import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import play.api.mvc._
import views.html.agent.tasklist.addbusiness.BusinessAlreadyRemoved

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BusinessAlreadyRemovedController @Inject()(identify: IdentifierAction,
                                                 businessAlreadyRemoved: BusinessAlreadyRemoved)
                                                (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpBaseController {

  def show(): Action[AnyContent] = identify { implicit request =>
    Ok(businessAlreadyRemoved())
  }

}