/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.individual.BaseClaimEnrolmentController
import config.AppConfig
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.NotFoundException
import views.html.individual.claimenrolment.ClaimEnrolmentAlreadySignedUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimEnrolmentAlreadySignedUpController @Inject()(val authService: AuthService,
                                                        val auditingService: AuditingService,
                                                        claimEnrolmentAlreadySignedUp: ClaimEnrolmentAlreadySignedUp)
                                                       (implicit val ec: ExecutionContext,
                                                        val appConfig: AppConfig,
                                                        mcc: MessagesControllerComponents) extends BaseClaimEnrolmentController with FeatureSwitching {
  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(ClaimEnrolment)) {
        Future.successful(
          Ok(claimEnrolmentAlreadySignedUp())
        )
      } else {
        throw new NotFoundException("[ClaimEnrolmentAlreadySignedUpController][show] - The claim enrolment feature switch is disabled")
      }
  }
}

