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

package controllers.individual.iv

import auth.individual.JourneyState.{RequestFunctions, SessionFunctions}
import auth.individual.{JourneyState, StatelessController, ClaimEnrolment => ClaimEnrolmentJourney}
import common.Constants.ITSASessionKeys
import config.AppConfig
import models.audits.IVOutcomeSuccessAuditing.IVOutcomeSuccessAuditModel
import play.api.mvc._
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IVSuccessController @Inject()(val appConfig: AppConfig,
                                    val authService: AuthService,
                                    val auditingService: AuditingService)
                                   (implicit val ec: ExecutionContext,
                                    mcc: MessagesControllerComponents) extends StatelessController {

  def success: Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      if (request.session.get(ITSASessionKeys.IdentityVerificationFlag).nonEmpty) {
        val nino: String = user.nino.getOrElse(throw new InternalServerException("[IVSuccessController][success] - Could not retrieve nino after iv success"))
        auditingService.audit(IVOutcomeSuccessAuditModel(nino))
      }
      if (request.session.isInState(ClaimEnrolmentJourney)) {
        Future.successful(
          Redirect(controllers.individual.claimenrolment.routes.ClaimEnrolmentResolverController.resolve)
            .removingFromSession(ITSASessionKeys.IdentityVerificationFlag)
        )
      } else {
        Future.successful(
          Redirect(controllers.individual.matching.routes.HomeController.home)
            .removingFromSession(ITSASessionKeys.IdentityVerificationFlag)
        )
      }
  }

  implicit val cacheSessionFunctions: Session => SessionFunctions = JourneyState.SessionFunctions
  implicit val cacheRequestFunctions: Request[_] => RequestFunctions = JourneyState.RequestFunctions


}
