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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import controllers.utils.ReferenceRetrieval
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.IncomeSourcesStatus
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.incometax.incomesource.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(yourIncomeSourceToSignUp: YourIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController  with ReferenceRetrieval {



  def show(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
    Ok(view)
  }



  def backUrl: String = controllers.individual.business.routes.TaskListController.show().url


  private def view(implicit request: Request[_]): Html =
    yourIncomeSourceToSignUp(
      backUrl = backUrl
  )

}
