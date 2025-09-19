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
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.common.OverseasPropertyModel
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
                                                           mcc: MessagesControllerComponents) extends SignUpController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withProperty(request.reference) { property =>
      Future.successful(Ok(view(
        viewModel = property,
        postAction = controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit, property.confirmed, property.startDateBeforeLimit),
        isGlobalEdit = isGlobalEdit
      )))
    }
  }

  def submit(isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withProperty(request.reference) { property =>
      if (property.isComplete) {
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

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, isConfirmed: Boolean, startDateBeforeLimit: Option[Boolean]): String = {
    if (isGlobalEdit && isConfirmed) {
      controllers.individual.routes.GlobalCheckYourAnswersController.show.url
    } else if (isGlobalEdit || isEditMode) {
      controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    } else {
      startDateBeforeLimit match {
        case Some(false) => routes.ForeignPropertyStartDateController.show().url
        case _ => routes.ForeignPropertyStartDateBeforeLimitController.show().url
      }
    }
  }

  private def withProperty(reference: String)(f: OverseasPropertyModel => Future[Result])(implicit hc: HeaderCarrier) =
    subscriptionDetailsService.fetchOverseasProperty(reference).flatMap {
      case None => Future.failed(new InternalServerException("[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details"))
      case Some(property) => f(property)
    }

}
