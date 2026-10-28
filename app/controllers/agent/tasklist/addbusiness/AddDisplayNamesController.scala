/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.mvc.*
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.addbusiness.AddDisplayNames
import forms.common.DisplayNameForm

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddDisplayNamesController @Inject()(view: AddDisplayNames,
                                          identify: IdentifierAction,
                                          journeyRefiner: ConfirmedClientJourneyRefiner,
                                          subscriptionDetailsService: SubscriptionDetailsService
                                         )(val appConfig: AppConfig)
                                         (implicit ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpBaseController {


  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchAllSoleTraderBusinesses(request.reference) map { soleTraderBusinesses =>
      val businessesWithDuplicateNames = soleTraderBusinesses.getBusinessesWithDuplicateNames
      Ok(view(
        displayNameForm = DisplayNameForm.displayNameForm,
        postAction = controllers.agent.tasklist.addbusiness.routes.AddDisplayNamesController.submit,
        businesses = businessesWithDuplicateNames
      ))
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchAllSoleTraderBusinesses(request.reference).flatMap { soleTraderBusinesses =>
      val businessesWithDuplicateNames = soleTraderBusinesses.getBusinessesWithDuplicateNames

      DisplayNameForm.validateDisplayNames(
        DisplayNameForm.displayNameForm.bindFromRequest(),
        businessesWithDuplicateNames.size
      ).fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            formWithErrors,
            controllers.agent.tasklist.addbusiness.routes.AddDisplayNamesController.submit,
            businessesWithDuplicateNames))),
        displayNames =>
          val businessesWithDisplayNames = soleTraderBusinesses.addDisplayNames(displayNames)
          subscriptionDetailsService.saveBusinesses(request.reference, businessesWithDisplayNames.businesses) map {
            case Right(_) => Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
            case Left(_) => throw new InternalServerException("[AddDisplayNamesController][submit] - failed to save business display names")
          }
      )
    }
  }
}
