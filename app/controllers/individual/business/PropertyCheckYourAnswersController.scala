/*
 * Copyright 2021 HM Revenue & Customs
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
import com.google.inject.Inject
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import models.common.PropertyModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import views.html.individual.incometax.business.PropertyCheckYourAnswers

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyCheckYourAnswersController @Inject()(val propertyCheckYourAnswersView: PropertyCheckYourAnswers,
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
        if (isEnabled(SaveAndRetrieve)) {
          withProperty(reference) { property =>
            Future.successful(Ok(
              propertyCheckYourAnswersView(
                viewModel = property,
                controllers.individual.business.routes.PropertyCheckYourAnswersController.submit(),
                backUrl(isEditMode)
              )
            ))
          }
        } else {
          Future.failed(new NotFoundException("[PropertyCheckYourAnswersController][show] - The save and retrieve feature switch is disabled"))
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          withProperty(reference) { property =>
            subscriptionDetailsService.saveProperty(reference, property.copy(confirmed = true)).map(_ => {
              Redirect(routes.TaskListController.show())
            })
          }
        } else {
          Future.failed(new NotFoundException("[PropertyCheckYourAnswersController][submit] - The save and retrieve feature switch is disabled"))
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.business.routes.TaskListController.show().url
    } else {
      controllers.individual.business.routes.PropertyAccountingMethodController.show().url
    }
  }

  private def withProperty(reference: String)(f: PropertyModel => Future[Result])(implicit hc: HeaderCarrier) = {
    subscriptionDetailsService.fetchProperty(reference).flatMap { maybeProperty =>
      val property = maybeProperty.getOrElse(
        throw new InternalServerException("[PropertyCheckYourAnswersController] - Could not retrieve property details")
      )

      f(property)
    }
  }
}
