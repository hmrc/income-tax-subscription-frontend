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

package controllers.individual.claimenrolment

import auth.individual.JourneyState.ResultFunctions
import auth.individual.{BaseClaimEnrolmentController, ClaimEnrolment => ClaimEnrolmentJourney}
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService}
import views.html.individual.claimenrolment.AddMTDITOverview

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddMTDITOverviewController @Inject()(addmtdit: AddMTDITOverview,
                                           val auditingService: AuditingService,
                                           val authService: AuthService)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends BaseClaimEnrolmentController {


  def show: Action[AnyContent] = Authenticated.unrestricted { implicit request =>
    _ =>
      Ok(addmtdit(postAction = routes.AddMTDITOverviewController.submit)).withJourneyState(ClaimEnrolmentJourney)
  }

  def submit: Action[AnyContent] = Authenticated { _ =>
    _ =>
      Redirect(routes.ClaimEnrolmentResolverController.resolve)
  }

}
