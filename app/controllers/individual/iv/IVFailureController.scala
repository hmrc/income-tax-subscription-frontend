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

package controllers.individual.iv

import auth.individual.StatelessController
import config.AppConfig
import config.featureswitch.FeatureSwitch.IdentityVerification
import config.featureswitch.FeatureSwitching
import javax.inject.Inject
import models.audits.IVOutcomeFailureAuditing.IVOutcomeFailureAuditModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.NotFoundException
import utilities.ITSASessionKeys
import views.html.individual.iv.IVFailure

import scala.concurrent.{ExecutionContext, Future}

class IVFailureController @Inject()(val authService: AuthService,
                                    val auditingService: AuditingService,
                                    ivFailure: IVFailure)
                                   (implicit val ec: ExecutionContext,
                                    val appConfig: AppConfig,
                                    mcc: MessagesControllerComponents) extends StatelessController with FeatureSwitching {

  def view(implicit request: Request[_]): Html = ivFailure()

  def failure: Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      if (isEnabled(IdentityVerification)) {
        if (request.session.get(ITSASessionKeys.IdentityVerificationFlag).nonEmpty) {
          request.getQueryString("journeyId").foreach(id => auditingService.audit(IVOutcomeFailureAuditModel(id)))
          Future.successful(Ok(view).removingFromSession(ITSASessionKeys.IdentityVerificationFlag))
        } else {
          Future.successful(Ok(view))
        }
      } else {
        Future.failed(new NotFoundException("[IVFailureController][failure] - identity verification disabled"))
      }
  }

}
