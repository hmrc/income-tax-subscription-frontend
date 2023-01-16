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

package controllers.agent.eligibility

import auth.agent.StatelessController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import utilities.AccountingPeriodUtil
import views.html.agent.eligibility.CannotSignUpThisYear

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CannotSignUpThisYearController @Inject()(val auditingService: AuditingService,
                                               val authService: AuthService,
                                               cannotSignUp: CannotSignUpThisYear)
                                              (implicit val appConfig: AppConfig,
                                      mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext) extends StatelessController  {
  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(cannotSignUp(AccountingPeriodUtil.getNextTaxYear))
  }
}
