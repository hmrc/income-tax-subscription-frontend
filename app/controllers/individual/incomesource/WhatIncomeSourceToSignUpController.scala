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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{SaveAndRetrieve, ForeignProperty => ForeignPropertyFeature}
import controllers.utils.ReferenceRetrieval
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.IncomeSourcesStatus
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import views.html.individual.incometax.incomesource.WhatIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIncomeSourceToSignUpController @Inject()(whatIncomeSourceToSignUp: WhatIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController  with ReferenceRetrieval {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        withReference { reference =>
          withIncomeSourceStatuses(reference) { incomeSourcesStatus =>
            Ok(view(businessIncomeSourceForm(incomeSourcesStatus), incomeSourcesStatus))
          }
        }
      } else {
        throw new NotFoundException("[WhatIncomeSourceToSignUpController][show] - The save and retrieve feature switch is disabled")
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        withReference { reference =>
          withIncomeSourceStatuses(reference) { incomeSourcesStatus =>
            businessIncomeSourceForm(incomeSourcesStatus).bindFromRequest.fold(
              formWithErrors => BadRequest(view(form = formWithErrors, incomeSourcesStatus)),
              {
                case SelfEmployed => Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
                case UkProperty => Redirect(controllers.individual.business.routes.PropertyStartDateController.show())
                case OverseasProperty if isEnabled(ForeignPropertyFeature) =>
                  Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
                case _ => throw new InternalServerException("[WhatIncomeSourceToSignUpController][submit] - The foreign property feature switch is disabled")
              }
            )
          }
        }
      } else {
        throw new NotFoundException("[WhatIncomeSourceToSignUpController][submit] - The save and retrieve feature switch is disabled")
      }
  }

  def backUrl: String = controllers.individual.business.routes.TaskListController.show().url

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
    subscriptionDetailsService.fetchOverseasProperty(reference) map (_.isEmpty && isEnabled(ForeignPropertyFeature))
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
      postAction = controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.submit(),
      backUrl = backUrl,
      incomeSourcesStatus = incomeSourcesStatus
    )

  private def businessIncomeSourceForm(incomeSourcesStatus: IncomeSourcesStatus): Form[BusinessIncomeSource] =
    BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus)

}
