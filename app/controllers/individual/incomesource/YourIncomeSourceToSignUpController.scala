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
import controllers.utils.ReferenceRetrieval
import models.common.business.SelfEmploymentData
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import views.html.individual.incometax.incomesource.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class YourIncomeSourceToSignUpController @Inject()(yourIncomeSourceToSignUp: YourIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {


  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        for {
          businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
          ukProperty <- subscriptionDetailsService.fetchProperty(reference)
          foreignProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
        } yield {
          Ok(view(
            selfEmployments = businesses,
            ukProperty = ukProperty,
            foreignProperty = foreignProperty
          ))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Redirect(controllers.individual.business.routes.TaskListController.show())
  }

  def backUrl: String = controllers.individual.business.routes.TaskListController.show().url


  private def view(selfEmployments: Seq[SelfEmploymentData],
                   ukProperty: Option[PropertyModel],
                   foreignProperty: Option[OverseasPropertyModel])(implicit request: Request[_]): Html =
    yourIncomeSourceToSignUp(
      postAction = routes.YourIncomeSourceToSignUpController.submit,
      backUrl = backUrl,
      selfEmployments,
      ukProperty,
      foreignProperty
    )

}
