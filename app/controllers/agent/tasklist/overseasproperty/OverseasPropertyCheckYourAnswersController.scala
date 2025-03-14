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

package controllers.agent.tasklist.overseasproperty

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.common.OverseasPropertyModel
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyCheckYourAnswersController @Inject()(identify: IdentifierAction,
                                                           journeyRefiner: ConfirmedClientJourneyRefiner,
                                                           subscriptionDetailsService: SubscriptionDetailsService,
                                                           view: OverseasPropertyCheckYourAnswers,
                                                           val appConfig: AppConfig)
                                                          (implicit cc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withOverseasProperty(request.reference) { overseasProperty =>
      Future.successful(Ok(view(
        viewModel = overseasProperty,
        postAction = routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit),
        isGlobalEdit = isGlobalEdit,
        backUrl = backUrl(isEditMode, isGlobalEdit, overseasProperty.confirmed),
        clientDetails = request.clientDetails
      )))
    }
  }

  def submit(isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withOverseasProperty(request.reference) {
      case overseasProperty if overseasProperty.isComplete =>
        subscriptionDetailsService.saveOverseasProperty(request.reference, overseasProperty.copy(confirmed = true)) map {
          case Right(_) =>
            if (isGlobalEdit) {
              Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
            } else {
              Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
            }
          case Left(_) => throw new InternalServerException("[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm overseas property")
        }
      case _ => Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
    }
  }

  private def withOverseasProperty(reference: String)(f: OverseasPropertyModel => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    subscriptionDetailsService.fetchOverseasProperty(reference) flatMap { maybeProperty =>
      f(maybeProperty.getOrElse(
        throw new InternalServerException("[OverseasPropertyCheckYourAnswersController] - Could not retrieve overseas property details")
      ))
    }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, confirmed: Boolean): String = {
    if (isGlobalEdit && confirmed) {
      controllers.agent.routes.GlobalCheckYourAnswersController.show.url
    } else if (isEditMode || isGlobalEdit) {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    } else {
      controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show().url
    }
  }
}