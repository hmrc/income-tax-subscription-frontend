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

package controllers.individual.tasklist.overseasproperty

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.common.OverseasPropertyModel
import models.requests.individual.SignUpRequest
import models.{No, Yes, YesNo}
import play.api.mvc._
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyCheckYourAnswersController @Inject()(view: OverseasPropertyCheckYourAnswers,
                                                           identify: IdentifierAction,
                                                           journeyRefiner: SignUpJourneyRefiner,
                                                           subscriptionDetailsService: SubscriptionDetailsService)
                                                          (val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val appConfig: AppConfig)
                                                          (implicit val ec: ExecutionContext,
                                                           mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withProperty(request.reference) { property =>
      if (isEnabled(RemoveAccountingMethod)) {
        subscriptionDetailsService.fetchForeignPropertyStartDateBeforeLimit(request.reference).map {
          case Some(resultSaved) =>
            Ok(view(
              viewModel = property,
              postAction = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
              backUrl = backUrl(isEditMode, isGlobalEdit, property.confirmed, Some(resultSaved)),
              isGlobalEdit = isGlobalEdit
            ))
        }
      } else {
        Future.successful {
          Ok(view(
            viewModel = property,
            postAction = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(isEditMode, isGlobalEdit, property.confirmed, None),
            isGlobalEdit = isGlobalEdit
          ))
        }
      }
    }
  }
  def submit(isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withProperty(request.reference) { property =>
      if (property.isComplete(isEnabled(RemoveAccountingMethod))) {
        subscriptionDetailsService.saveOverseasProperty(request.reference, property.copy(confirmed = true)) map {
          case Right(_) =>
            if (isGlobalEdit) {
              Redirect(controllers.individual.routes.GlobalCheckYourAnswersController.show)
            } else {
              Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
            }
          case Left(_) => throw new InternalServerException("[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm property details")
        }
      } else {
        Future.successful(Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    }
  }
  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, isConfirmed: Boolean, resultSaved: Option[YesNo])(implicit request: SignUpRequest[_]): String = {
    if (isEnabled(RemoveAccountingMethod) && isGlobalEdit && isConfirmed) {
      controllers.individual.routes.GlobalCheckYourAnswersController.show.url
    } else if (isEnabled(RemoveAccountingMethod) && isGlobalEdit || isEditMode) {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    } else if (isEnabled(RemoveAccountingMethod)) {
      resultSaved match {
        case Some(Yes) => controllers.individual.tasklist.overseasproperty.routes.ForeignPropertyStartDateBeforeLimitController.show().url
        case Some(No) => controllers.individual.tasklist.overseasproperty.routes.ForeignPropertyStartDateController.show().url
      }
    } else {
      controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show().url
    }
  }

  private def withProperty(reference: String)(f: OverseasPropertyModel => Future[Result])(implicit hc: HeaderCarrier) =
    subscriptionDetailsService.fetchOverseasProperty(reference).flatMap {
      case None => Future.failed(new InternalServerException("[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details"))
      case Some(property) => f(property)
    }

}
