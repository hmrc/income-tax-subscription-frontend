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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import controllers.utils.ReferenceRetrieval
import models.common.OverseasPropertyModel
import play.api.mvc._
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.incometax.business.OverseasPropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyCheckYourAnswersController @Inject()(val view: OverseasPropertyCheckYourAnswers,
                                                           val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService
                                                          )(implicit val ec: ExecutionContext,
                                                            val appConfig: AppConfig,
                                                            mcc: MessagesControllerComponents
                                                          ) extends SignUpController with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        withProperty(reference) { property =>
          Future.successful(Ok(view(
            property,
            controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.submit(),
            backUrl(isEditMode)
          )))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        withProperty(reference) {
          case property@OverseasPropertyModel(Some(_), Some(_), _) =>
            subscriptionDetailsService.saveOverseasProperty(reference, property.copy(confirmed = true)) map {
              case Right(_) => Redirect(continueLocation)
              case Left(_) => throw new InternalServerException("[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm property details")
            }
          case _ => Future.successful(Redirect(continueLocation))
        }
      }
  }

  def continueLocation: Call = {
    if (isEnabled(EnableTaskListRedesign)) {
      controllers.individual.incomesource.routes.YourIncomeSourceToSignUpController.show
    } else {
      routes.TaskListController.show()
    }
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode) {
      continueLocation.url
    } else {
      controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url
    }

  private def withProperty(reference: String)(f: OverseasPropertyModel => Future[Result])(implicit hc: HeaderCarrier) =
    subscriptionDetailsService.fetchOverseasProperty(reference).flatMap {
      case None => Future.failed(new InternalServerException("[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details"))
      case Some(property) => f(property)
    }
}
