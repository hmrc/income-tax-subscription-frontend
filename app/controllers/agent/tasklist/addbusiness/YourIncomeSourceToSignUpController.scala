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

package controllers.agent.tasklist.addbusiness

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class YourIncomeSourceToSignUpController @Inject()(view: YourIncomeSourceToSignUp,
                                                   identify: IdentifierAction,
                                                   journeyRefiner: ConfirmedClientJourneyRefiner,
                                                   subscriptionDetailsService: SubscriptionDetailsService
                                                  )(val appConfig: AppConfig)
                                                  (implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {


  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      incomeSources <- subscriptionDetailsService.fetchAllIncomeSources(request.reference)
      prePopFlag <- subscriptionDetailsService.fetchPrePopFlag(request.reference)
    } yield {
      Ok(view(
        postAction = routes.YourIncomeSourceToSignUpController.submit,
        backUrl = backUrl,
        clientDetails = request.clientDetails,
        incomeSources = incomeSources,
        prepopulated = prePopFlag.contains(true)
      ))
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchAllIncomeSources(request.reference) flatMap { incomeSources =>
      if (incomeSources.isComplete) {
        subscriptionDetailsService.saveIncomeSourcesConfirmation(request.reference) map {
          case Right(_) => Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
          case Left(_) => throw new InternalServerException("[YourIncomeSourceToSignUpController][submit] - failed to save income sources confirmation")
        }
      } else {
        Future.successful(Redirect(controllers.agent.tasklist.routes.IncomeSourcesIncompleteController.show))
      }
    }
  }

  def backUrl: String = {
    controllers.agent.routes.WhatYouNeedToDoController.show().url
  }
}
