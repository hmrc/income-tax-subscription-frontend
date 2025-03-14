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

package controllers.individual.tasklist

import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.individual.tasklist.IncomeSourcesIncomplete

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IncomeSourcesIncompleteController @Inject()(identify: IdentifierAction,
                                                  journeyRefiner: SignUpJourneyRefiner,
                                                  view: IncomeSourcesIncomplete)
                                                 (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      postAction = controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.submit,
      backUrl = controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    ))
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) { _ =>
    Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
  }

}
