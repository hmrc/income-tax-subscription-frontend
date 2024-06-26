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

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.PropertyModel
import play.api.mvc._
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.ukproperty.PropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyCheckYourAnswersController @Inject()(propertyCheckYourAnswersView: PropertyCheckYourAnswers)
                                                  (val auditingService: AuditingService,
                                                   val sessionDataService: SessionDataService,
                                                   val appConfig: AppConfig,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        withProperty(reference) { property =>
          Future.successful(Ok(
            propertyCheckYourAnswersView(
              viewModel = property,
              routes.PropertyCheckYourAnswersController.submit(),
              backUrl(isEditMode),
              clientDetails = request.clientDetails
            )
          ))
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        withProperty(reference) { property =>
          if (property.accountingMethod.isDefined && property.startDate.isDefined) {
            subscriptionDetailsService.saveProperty(reference, property.copy(confirmed = true)) map {
              case Right(_) => Redirect(continueLocation)
              case Left(_) => throw new InternalServerException("[PropertyCheckYourAnswersController][submit] - Could not confirm property")
            }
          } else {
            Future.successful(Redirect(continueLocation))
          }
        }
      }
  }

  def continueLocation: Call = {
    controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      continueLocation.url
    } else {
      routes.PropertyAccountingMethodController.show().url

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
