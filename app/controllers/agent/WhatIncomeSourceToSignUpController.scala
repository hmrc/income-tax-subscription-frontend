/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{SaveAndRetrieve, ForeignProperty => ForeignPropertyFeature}
import controllers.utils.ReferenceRetrieval
import forms.agent.BusinessIncomeSourceForm
import models.IncomeSourcesStatus
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import play.api.data.Form
import play.api.mvc._
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import views.html.agent.WhatIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIncomeSourceToSignUpController @Inject()(val whatIncomeSourceToSignUp: WhatIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService
                                                  )(implicit val ec: ExecutionContext,
                                                    val appConfig: AppConfig,
                                                    mcc: MessagesControllerComponents) extends AuthenticatedController
  
  with ReferenceRetrieval {
  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        withAgentReference { reference =>
          withIncomeSourceStatuses(reference) {
            case IncomeSourcesStatus(false, false, false) => Redirect(controllers.agent.routes.TaskListController.show())
            case incomeSourcesStatus => Ok(view(businessIncomeSourceForm(incomeSourcesStatus), incomeSourcesStatus))
          }
        }
      } else {
        throw new NotFoundException("[WhatIncomeSourceToSignUpController][show] - The save and retrieve feature switch is disabled")
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        withAgentReference { reference =>
          withIncomeSourceStatuses(reference) { incomeSourcesStatus =>
            businessIncomeSourceForm(incomeSourcesStatus).bindFromRequest.fold(
              formWithErrors => BadRequest(view(form = formWithErrors, incomeSourcesStatus)),
              {
                case SelfEmployed => Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
                case UkProperty => Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
                case OverseasProperty if isEnabled(ForeignPropertyFeature) =>
                  Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
                case _ => throw new InternalServerException("[WhatIncomeSourceToSignUpController][submit] - The foreign property feature switch is disabled")
              }
            )
          }
        }
      } else {
        throw new NotFoundException("[WhatIncomeSourceToSignUpController][submit] - The save and retrieve feature switch is disabled")
      }
  }

  private def withIncomeSourceStatuses(reference: String)(f: IncomeSourcesStatus => Result)(implicit hc: HeaderCarrier): Future[Result] =
    for {
      selfEmploymentAvailable <- selfEmploymentAvailable(reference)
      ukPropertyAvailable <- ukPropertyAvailable(reference)
      overseasPropertyAvailable <- overseasPropertyAvailable(reference)
    } yield {
      f(IncomeSourcesStatus(
        selfEmploymentAvailable = selfEmploymentAvailable,
        ukPropertyAvailable = ukPropertyAvailable,
        overseasPropertyAvailable = overseasPropertyAvailable
      ))
    }

  private def selfEmploymentAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    subscriptionDetailsService.fetchAllSelfEmployments(reference).map {
      case Some(selfEmployments) => selfEmployments.length < appConfig.maxSelfEmployments
      case None => true
    }
  }

  private def ukPropertyAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    subscriptionDetailsService.fetchProperty(reference) map (_.isEmpty)
  }

  private def overseasPropertyAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (isEnabled(ForeignPropertyFeature)) {
      subscriptionDetailsService.fetchOverseasProperty(reference) map (_.isEmpty)
    } else {
      Future.successful(false)
    }
  }

  private def view(form: Form[BusinessIncomeSource], incomeSourcesStatus: IncomeSourcesStatus)(implicit request: Request[_]) = {
    whatIncomeSourceToSignUp(
      form,
      incomeSourcesStatus,
      postAction = controllers.agent.routes.WhatIncomeSourceToSignUpController.submit(),
      backUrl = controllers.agent.routes.TaskListController.show().url
    )
  }

  private def businessIncomeSourceForm(incomeSourcesStatus: IncomeSourcesStatus): Form[BusinessIncomeSource] =
    BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus)
}
