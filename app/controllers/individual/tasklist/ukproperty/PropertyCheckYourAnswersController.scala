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

package controllers.individual.tasklist.ukproperty

import config.AppConfig
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.common.PropertyModel
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.tasklist.ukproperty.PropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyCheckYourAnswersController @Inject()(identify: IdentifierAction,
                                                   journeyRefiner: SignUpJourneyRefiner,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   view: PropertyCheckYourAnswers)
                                                  (val appConfig: AppConfig)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withProperty(request.reference) { property =>
      Future.successful(Ok(view(
        viewModel = property,
        postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit, property.confirmed, property.startDateBeforeLimit.contains(true)),
        isGlobalEdit = isGlobalEdit
      )))
    }
  }

  def submit(isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withProperty(request.reference) { property =>
      if (property.isComplete) {
        subscriptionDetailsService.saveProperty(request.reference, property.copy(confirmed = true)) map {
          case Right(_) =>
            if (isGlobalEdit) {
              Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
            } else {
              Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
            }
          case Left(_) =>
            throw new InternalServerException("[PropertyCheckYourAnswersController][submit] - Could not confirm property")
        }
      } else {
        Future.successful(Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    }
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, isConfirmed: Boolean, propertyStartDateBeforeLimit: Boolean): String = {
    if (isGlobalEdit && isConfirmed) {
      controllers.individual.routes.GlobalCheckYourAnswersController.show.url
    } else if (isEditMode || isGlobalEdit) {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    } else {
      if (propertyStartDateBeforeLimit) {
        controllers.individual.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show().url
      } else {
        controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url
      }
    }
  }

  private def withProperty(reference: String)(f: PropertyModel => Future[Result])(implicit hc: HeaderCarrier) = {
    subscriptionDetailsService.fetchProperty(reference).flatMap { maybeProperty =>
      f(maybeProperty.getOrElse(
        throw new InternalServerException("[PropertyCheckYourAnswersController] - Could not retrieve property details")
      ))
    }
  }


}
