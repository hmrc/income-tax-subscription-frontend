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
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.NotFoundException
import views.html.individual.incometax.claimEnrolment.AddMTDITOverview

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddMTDITOverviewController @Inject()(addmtdit: AddMTDITOverview,
                                           val auditingService: AuditingService,
                                          val authService: AuthService)
                                         (implicit val ec: ExecutionContext,
                                          implicit val appConfig: AppConfig,
                                          mcc: MessagesControllerComponents) extends StatelessController with FeatureSwitching {


 def show: Action[AnyContent] = Authenticated.async {implicit request =>
   implicit user =>
      if (isEnabled(ClaimEnrolment)) {
        Future.successful(
        Ok(addmtdit(postAction = controllers.individual.claimEnrolment.routes.AddMTDITOverviewController.submit())
      ))
      }else {
        throw new NotFoundException("[AddMTDITOverviewController][show] - ClaimEnrolment Enabled feature switch not enabled")
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(
      Redirect(controllers.individual.claimEnrolment.routes.AddMTDITOverviewController.show())
      )
  }

}
