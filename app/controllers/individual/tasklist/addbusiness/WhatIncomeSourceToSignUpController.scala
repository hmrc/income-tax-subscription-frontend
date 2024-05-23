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

package controllers.individual.tasklist.addbusiness

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.IncomeSourcesStatus
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.tasklist.addbusiness.WhatIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIncomeSourceToSignUpController @Inject()(whatIncomeSourceToSignUp: WhatIncomeSourceToSignUp)
                                                  (val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val sessionDataService: SessionDataService,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withIndividualReference { reference =>
        withIncomeSourceStatuses(reference) {
          case IncomeSourcesStatus(false, false, false) => Redirect(controllers.individual.tasklist.routes.TaskListController.show())
          case incomeSourcesStatus => Ok(view(businessIncomeSourceForm(incomeSourcesStatus), incomeSourcesStatus))
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withIndividualReference { reference =>
        withIncomeSourceStatuses(reference) { incomeSourcesStatus =>
          businessIncomeSourceForm(incomeSourcesStatus).bindFromRequest().fold(
            formWithErrors => BadRequest(view(form = formWithErrors, incomeSourcesStatus)),
            {
              case SelfEmployed => Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
              case UkProperty => Redirect(controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show())
              case OverseasProperty => Redirect(controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show())
              case _ => throw new InternalServerException("[WhatIncomeSourceToSignUpController][submit] - The foreign property feature switch is disabled")
            }
          )
        }
      }
  }

  def backUrl: String = controllers.individual.tasklist.routes.TaskListController.show().url

  private def selfEmploymentAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    subscriptionDetailsService.fetchAllSelfEmployments(reference).map { case (businesses, _) =>
      businesses.length < appConfig.maxSelfEmployments
    }
  }

  private def ukPropertyAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    subscriptionDetailsService.fetchProperty(reference) map (_.isEmpty)
  }

  private def overseasPropertyAvailable(reference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    subscriptionDetailsService.fetchOverseasProperty(reference) map (_.isEmpty)
  }

  private def withIncomeSourceStatuses(reference: String)(f: IncomeSourcesStatus => Result)(implicit hc: HeaderCarrier): Future[Result] = {
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
  }

  private def view(form: Form[BusinessIncomeSource], incomeSourcesStatus: IncomeSourcesStatus)(implicit request: Request[_]): Html =
    whatIncomeSourceToSignUp(
      incomeSourceForm = form,
      postAction = controllers.individual.tasklist.addbusiness.routes.WhatIncomeSourceToSignUpController.submit(),
      backUrl = backUrl,
      incomeSourcesStatus = incomeSourcesStatus
    )

  private def businessIncomeSourceForm(incomeSourcesStatus: IncomeSourcesStatus): Form[BusinessIncomeSource] =
    BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus)

}
