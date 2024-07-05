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

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.OverseasPropertyModel
import play.api.mvc._
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyCheckYourAnswersController @Inject()(overseasPropertyCheckYourAnswersView: OverseasPropertyCheckYourAnswers,
                                                           subscriptionDetailsService: SubscriptionDetailsService,
                                                           clientDetailsRetrieval: ClientDetailsRetrieval,
                                                           referenceRetrieval: ReferenceRetrieval)
                                                          (val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val appConfig: AppConfig)
                                                          (implicit val ec: ExecutionContext,
                                                           mcc: MessagesControllerComponents) extends AuthenticatedController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        withOverseasProperty(reference) { property =>
          clientDetailsRetrieval.getClientDetails map { clientDetails =>
            Ok(overseasPropertyCheckYourAnswersView(
              viewModel = property,
              routes.OverseasPropertyCheckYourAnswersController.submit(),
              backUrl(isEditMode),
              clientDetails = clientDetails
            ))
          }
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        withOverseasProperty(reference) { property =>
          if (property.accountingMethod.isDefined && property.startDate.isDefined) {
            subscriptionDetailsService.saveOverseasProperty(reference, property.copy(confirmed = true)) map {
              case Right(_) => Redirect(continueLocation)
              case Left(_) => throw new InternalServerException("[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm property details")
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
      routes.OverseasPropertyAccountingMethodController.show().url
    }
  }

  private def withOverseasProperty(reference: String)(f: OverseasPropertyModel => Future[Result])(implicit hc: HeaderCarrier) = {
    subscriptionDetailsService.fetchOverseasProperty(reference).flatMap { maybeProperty =>
      val property = maybeProperty.getOrElse(
        throw new InternalServerException("[OverseasPropertyCheckYourAnswersController] - Could not retrieve property details")
      )
      f(property)
    }
  }
}
