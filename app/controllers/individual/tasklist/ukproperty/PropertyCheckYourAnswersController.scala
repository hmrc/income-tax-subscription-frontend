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

import auth.individual.SignUpController
import com.google.inject.Inject
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.PropertyModel
import play.api.mvc._
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.tasklist.ukproperty.PropertyCheckYourAnswers

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyCheckYourAnswersController @Inject()(propertyCheckYourAnswersView: PropertyCheckYourAnswers,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   referenceRetrieval: ReferenceRetrieval)
                                                  (val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val appConfig: AppConfig)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        withProperty(reference) { property =>
          Future.successful(Ok(
            propertyCheckYourAnswersView(
              viewModel = property,
              controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
              backUrl(isEditMode)
            )
          ))
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        withProperty(reference) {
          case property@PropertyModel(Some(_), Some(_), _) =>
            subscriptionDetailsService.saveProperty(reference, property.copy(confirmed = true)).map {
              case Right(_) => Redirect(continueLocation)
              case Left(_) => throw new InternalServerException("[PropertyCheckYourAnswersController][submit] - Could not confirm property")
            }
          case _ => Future.successful(Redirect(continueLocation))
        }
      }
  }

  def continueLocation: Call = {
    controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      continueLocation.url
    } else {
      controllers.individual.tasklist.ukproperty.routes.PropertyAccountingMethodController.show().url
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
