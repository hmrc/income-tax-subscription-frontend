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
import config.AppConfig
import config.featureswitch.FeatureSwitch.ForeignProperty
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import models.common.IncomeSourceModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RoutingController @Inject()(val auditingService: AuditingService,
                                  val authService: AuthService,
                                  val appConfig: AppConfig,
                                  val subscriptionDetailsService: SubscriptionDetailsService)
                                 (implicit val ec: ExecutionContext,
                                  mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching with ReferenceRetrieval {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchIncomeSource(reference) map {
          case Some(IncomeSourceModel(_, true, _)) =>
            Redirect(controllers.individual.business.routes.PropertyStartDateController.show())
          case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty) =>
            Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
          case _ =>
            Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show)
        }
      }
  }

}
