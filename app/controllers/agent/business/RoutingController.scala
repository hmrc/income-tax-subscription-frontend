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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.ForeignProperty
import controllers.utils.ReferenceRetrieval
import models.common.IncomeSourceModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoutingController @Inject()(val auditingService: AuditingService,
                                  val authService: AuthService,
                                  val subscriptionDetailsService: SubscriptionDetailsService)
                                 (implicit val ec: ExecutionContext,
                                  val appConfig: AppConfig,
                                  mcc: MessagesControllerComponents) extends AuthenticatedController  with ReferenceRetrieval {

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        if (editMode) {
          Future.successful(Redirect(controllers.agent.routes.CheckYourAnswersController.show))
        } else {
          subscriptionDetailsService.fetchIncomeSource(reference) map {
            case Some(IncomeSourceModel(_, true, _)) =>
              Redirect(routes.PropertyStartDateController.show())
            case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty) =>
              Redirect(routes.OverseasPropertyStartDateController.show())
            case _ =>
              Redirect(controllers.agent.routes.CheckYourAnswersController.show)
          }
        }
      }
  }

}
