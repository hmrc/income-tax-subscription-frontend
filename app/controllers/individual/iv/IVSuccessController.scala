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
import models.audits.IVOutcomeSuccessAuditing.IVOutcomeSuccessAuditModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.ITSASessionKeys

import scala.concurrent.{ExecutionContext, Future}

class IVSuccessController @Inject()(val appConfig: AppConfig,
                                    val authService: AuthService,
                                    val auditingService: AuditingService)
                                   (implicit val ec: ExecutionContext,
                                    mcc: MessagesControllerComponents) extends StatelessController with FeatureSwitching {

  def success: Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      if (isEnabled(IdentityVerification)) {
        if (request.session.get(ITSASessionKeys.IdentityVerificationFlag).nonEmpty) {
          val nino: String = user.nino.getOrElse(throw new InternalServerException("[IVSuccessController][success] - Could not retrieve nino after iv success"))
          auditingService.audit(IVOutcomeSuccessAuditModel(nino))
          Future.successful(
            Redirect(controllers.usermatching.routes.HomeController.home())
              .removingFromSession(ITSASessionKeys.IdentityVerificationFlag)
          )
        } else {
          Future.successful(Redirect(controllers.usermatching.routes.HomeController.home()))
        }
      } else {
        Future.failed(new NotFoundException("[IVSuccessController][success] - identity verification disabled"))
      }
  }

}
