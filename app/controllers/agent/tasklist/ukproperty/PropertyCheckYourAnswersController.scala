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

package controllers.agent.tasklist.ukproperty

import config.AppConfig
import config.featureswitch.FeatureSwitch.AgentStreamline
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.common.PropertyModel
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.tasklist.ukproperty.PropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyCheckYourAnswersController @Inject()(identify: IdentifierAction,
                                                   journeyRefiner: ConfirmedClientJourneyRefiner,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   view: PropertyCheckYourAnswers,
                                                   val appConfig: AppConfig)
                                                  (implicit cc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController
  with FeatureSwitching {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withProperty(request.reference) { property =>
      Future.successful(Ok(view(
        viewModel = property,
        postAction = routes.PropertyCheckYourAnswersController.submit(isGlobalEdit),
        isGlobalEdit = isGlobalEdit,
        backUrl = backUrl(isEditMode, isGlobalEdit, property.confirmed),
        clientDetails = request.clientDetails
      )))
    }
  }

  def submit(isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withProperty(request.reference) { property =>
      if (property.accountingMethod.isDefined && property.startDate.isDefined) {
        subscriptionDetailsService.saveProperty(request.reference, property.copy(confirmed = true)) map {
          case Right(_) =>
            if (isGlobalEdit) {
              Redirect(controllers.agent.routes.GlobalCheckYourAnswersController.show)
            } else {
              Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
            }
          case Left(_) => throw new InternalServerException("[PropertyCheckYourAnswersController][submit] - Could not confirm property")
        }
      } else {
        Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    }
  }

  private def withProperty(reference: String)(f: PropertyModel => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    subscriptionDetailsService.fetchProperty(reference) flatMap { maybeProperty =>
      f(maybeProperty.getOrElse(
        throw new InternalServerException("[PropertyCheckYourAnswersController] - Could not retrieve property details")
      ))
    }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, confirmed: Boolean): String = {
    if (isGlobalEdit && confirmed) {
      controllers.agent.routes.GlobalCheckYourAnswersController.show.url
    } else if (isEditMode || isGlobalEdit) {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    } else if (isEnabled(AgentStreamline)) {
      routes.PropertyIncomeSourcesController.show().url
    } else {
      routes.PropertyAccountingMethodController.show().url
    }
  }

}