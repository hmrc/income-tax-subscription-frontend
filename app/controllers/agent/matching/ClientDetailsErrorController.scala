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

import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.agent.matching.ClientDetailsError

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsErrorController @Inject()(identify: IdentifierAction,
                                             clientDetailsError: ClientDetailsError)
                                            (implicit val ec: ExecutionContext,
                                             mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = identify { implicit request =>
    Ok(clientDetailsError())
  }

}
