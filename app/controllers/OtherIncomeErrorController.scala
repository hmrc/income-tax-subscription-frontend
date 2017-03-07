/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.Inject

import config.BaseControllerConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

class OtherIncomeErrorController @Inject()(implicit val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi) extends BaseController {

  val showOtherIncomeError = Action.async { implicit request =>
    Future.successful(Ok(views.html.other_income_error(postAction = controllers.routes.OtherIncomeErrorController.submitOtherIncomeError(), backUrl)))
  }

  val submitOtherIncomeError: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      Future.successful(Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod()))
  }

  lazy val backUrl: String = controllers.routes.OtherIncomeController.showOtherIncome().url


}
