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
import controllers.agent.actions.{IdentifierAction, SignPostedJourneyRefiner}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.agent.matching.NoClientRelationship

import javax.inject.{Inject, Singleton}

@Singleton
class NoClientRelationshipController @Inject()(identify: IdentifierAction,
                                               journeyRefiner: SignPostedJourneyRefiner,
                                               view: NoClientRelationship)
                                              (implicit mcc: MessagesControllerComponents) extends SignUpBaseController {

  val show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      postAction = controllers.agent.matching.routes.NoClientRelationshipController.submit,
      clientName = request.clientDetails.name,
      clientNino = request.clientDetails.formattedNino
    ))
  }

  val submit: Action[AnyContent] = identify { _ =>
    Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
  }

}
