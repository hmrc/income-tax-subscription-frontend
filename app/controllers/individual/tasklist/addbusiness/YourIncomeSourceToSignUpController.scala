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

package controllers.individual.tasklist.addbusiness

import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(identify: IdentifierAction,
                                                   journeyRefiner: SignUpJourneyRefiner,
                                                   view: YourIncomeSourceToSignUp,
                                                   subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    for {
      incomeSources <- subscriptionDetailsService.fetchAllIncomeSources(request.reference)
      maybePrePop <- subscriptionDetailsService.fetchPrePopFlag(request.reference)
    } yield {
      Ok(view(
        postAction = routes.YourIncomeSourceToSignUpController.submit,
        backUrl = backUrl,
        incomeSources,
        maybePrePop.contains(true)
      ))
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchAllIncomeSources(request.reference) flatMap { incomeSources =>
      if (incomeSources.isComplete) {
        subscriptionDetailsService.saveIncomeSourcesConfirmation(request.reference) map {
          case Right(_) => Redirect(continueLocation)
          case Left(_) => throw new InternalServerException("[YourIncomeSourceToSignUpController][submit] - failed to save income sources confirmation")
        }
      } else {
        Future.successful(Redirect(continueLocation))
      }
    }
  }

  def continueLocation: Call = {
    controllers.individual.routes.GlobalCheckYourAnswersController.show
  }

  def backUrl: String = {
    controllers.individual.routes.WhatYouNeedToDoController.show.url
  }


}
