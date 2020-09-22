/*
 * Copyright 2020 HM Revenue & Customs
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
import forms.individual.business.AccountingMethodForm
import javax.inject.{Inject, Singleton}
import models.common.{AccountingMethodModel, IncomeSourceModel}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataUtil.CacheMapUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoutingController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                 (implicit val ec: ExecutionContext,
                                  mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchIncomeSource() map {
        case Some(IncomeSourceModel(_, true, _)) =>
          Redirect(controllers.individual.business.routes.PropertyCommencementDateController.show())
        case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty) =>
          Redirect(controllers.individual.business.routes.OverseasPropertyCommencementDateController.show())
        case _ =>
          Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
      }
  }

}
