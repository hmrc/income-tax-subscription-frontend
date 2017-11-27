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

package incometax.business.controllers

import javax.inject.{Inject, Singleton}

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

@Singleton
class RegisterNextAccountingPeriodController @Inject()(val baseConfig: BaseControllerConfig,
                                                       val messagesApi: MessagesApi,
                                                       val keystoreService: KeystoreService,
                                                       val authService: AuthService
                                                      ) extends SignUpController {

  def view()(implicit request: Request[AnyContent]): Html =
    incometax.business.views.html.register_next_accounting_period(
      postAction = incometax.business.controllers.routes.RegisterNextAccountingPeriodController.submit(),
      backUrl = incometax.business.controllers.routes.BusinessAccountingPeriodPriorController.show().url
    )

  val show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(view)
  }

  val submit: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show())
  }

}
