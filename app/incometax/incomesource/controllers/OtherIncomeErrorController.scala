/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.incomesource.controllers

import javax.inject.{Inject, Singleton}

import core.audit.Logging
import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.subscription.models.{Both, Business, Property}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

@Singleton
class OtherIncomeErrorController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val logging: Logging,
                                           val authService: AuthService
                                          ) extends SignUpController {

  val show = Action.async { implicit request =>
    Future.successful(Ok(incometax.incomesource.views.html.other_income_error(
      postAction = incometax.incomesource.controllers.routes.OtherIncomeErrorController.submit(),
      backUrl
    )))
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cache <- keystoreService.fetchAll()
        optIncomeSource = cache.getIncomeSourceType()
      } yield optIncomeSource match {
        case Some(Business) =>
          Redirect(incometax.business.controllers.routes.BusinessNameController.show())
        case Some(Property) =>
          Redirect(incometax.subscription.controllers.routes.TermsController.show())
        case Some(Both) =>
          Redirect(incometax.business.controllers.routes.BusinessNameController.show())
        case _ =>
          Redirect(incometax.incomesource.controllers.routes.WorkForYourselfController.show())
      }
  }

  lazy val backUrl: String = incometax.incomesource.controllers.routes.OtherIncomeController.show().url

}
