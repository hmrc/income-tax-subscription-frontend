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

package controllers.agent

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.*
import play.api.mvc.*
import services.*
import views.html.agent.WhatYouNeedToDo

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouNeedToDoController @Inject()(view: WhatYouNeedToDo,
                                          identify: IdentifierAction,
                                          journeyRefiner: ConfirmedClientJourneyRefiner,
                                          eligibilityStatusService: GetEligibilityStatusService,
                                          subscriptionDetailsService: SubscriptionDetailsService
                                         )(val appConfig: AppConfig)
                                         (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      postAction = routes.WhatYouNeedToDoController.submit,
      clientName = request.clientDetails.name,
      clientNino = request.clientDetails.formattedNino
    ))
  }

  val submit: Action[AnyContent] = (identify andThen journeyRefiner) { _ =>
    Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
  }
}