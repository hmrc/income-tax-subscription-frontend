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

package controllers.individual.claimEnrolment

import auth.individual.StatelessController
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import views.html.individual.incometax.subscription.claimEnrolment.ClaimEnrolmentConfirmation
import auth.individual.JourneyState._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimEnrolmentController @Inject()(val authService: AuthService,
                                         val auditingService: AuditingService,
                                         claimEnrolmentConfirmation: ClaimEnrolmentConfirmation)
                                        (implicit val ec: ExecutionContext,
                                         val appConfig: AppConfig,
                                         mcc: MessagesControllerComponents) extends StatelessController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(claimEnrolmentConfirmation(
        postAction = controllers.individual.claimEnrolment.routes.ClaimEnrolmentController.submit()
      )))
  }

  def submit(): Action[AnyContent] = Action.async {
    Future.successful(Redirect(appConfig.btaUrl))
  }
}
