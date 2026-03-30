/*
 * Copyright 2026 HM Revenue & Customs
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

import auth.individual.SignUpController
import config.AppConfig
import play.api.mvc.*
import services.*
import utilities.AccountingPeriodUtil
import views.html.individual.tasklist.taxyear.NonEligibleMandated

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NonEligibleMandatedController @Inject()(view: NonEligibleMandated)
                                              (val auditingService: AuditingService,
                                               val appConfig: AppConfig,
                                               val authService: AuthService)
                                              (implicit mcc: MessagesControllerComponents, val ec: ExecutionContext) extends SignUpController {

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      val model = AccountingPeriodUtil.getCurrentTaxYear
      Ok(view(
        postAction = routes.NonEligibleMandatedController.submit,
        startYear = model.startDate.year.toInt,
        endYear = model.endDate.year.toInt
      ))
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
  }
}