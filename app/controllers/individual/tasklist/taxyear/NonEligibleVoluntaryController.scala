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

package controllers.individual.tasklist.taxyear

import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import config.AppConfig
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import play.api.mvc.*
import services.*
import utilities.AccountingPeriodUtil
import views.html.individual.tasklist.taxyear.NonEligibleVoluntary

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NonEligibleVoluntaryController @Inject()(view: NonEligibleVoluntary)
                                              (val appConfig: AppConfig,
                                               identify: IdentifierAction,
                                               refine: SignUpJourneyRefiner)
                                              (implicit mcc: MessagesControllerComponents,
                                               ec: ExecutionContext) extends SignUpBaseController with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen refine) { implicit request =>
    val model = AccountingPeriodUtil.getCurrentTaxYear
    Ok(view(
      postAction = routes.NonEligibleVoluntaryController.submit,
      startYear = model.startDate.year.toInt,
      endYear = model.endDate.year.toInt
    ))
  }

  def submit: Action[AnyContent] = (identify andThen refine) { implicit request =>
    Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
  }
}
