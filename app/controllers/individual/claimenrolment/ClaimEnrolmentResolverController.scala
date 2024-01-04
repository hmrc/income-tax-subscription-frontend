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

import auth.individual.BaseClaimEnrolmentController
import config.AppConfig
import models.audits.ClaimEnrolAddToIndivCredAuditing.ClaimEnrolAddToIndivCredAuditingModel
import play.api.mvc._
import services.individual.claimenrolment.ClaimEnrolmentService
import services.individual.claimenrolment.ClaimEnrolmentService.{AlreadySignedUp, ClaimEnrolmentError, NotSubscribed}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ClaimEnrolmentResolverController @Inject()(claimEnrolmentService: ClaimEnrolmentService,
                                                 val auditingService: AuditingService,
                                                 val authService: AuthService
                                                )
                                                (implicit val ec: ExecutionContext,
                                                 val appConfig: AppConfig,
                                                 mcc: MessagesControllerComponents) extends BaseClaimEnrolmentController {

  def resolve: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      claimEnrolmentService.claimEnrolment map {
        case Right(claimEnrolSuccess) => {
          auditingService.audit(ClaimEnrolAddToIndivCredAuditingModel(nino = claimEnrolSuccess.nino, mtditid = claimEnrolSuccess.mtditid))
          Redirect(controllers.individual.claimenrolment.sps.routes.SPSHandoffForClaimEnrolController.redirectToSPS)
        }
        case Left(NotSubscribed) => throw new InternalServerException("[ClaimEnrollmentResolverController] User was not subscribed")
        case Left(AlreadySignedUp) => Redirect(routes.ClaimEnrolmentAlreadySignedUpController.show)
        case Left(ClaimEnrolmentError(msg)) => throw new InternalServerException(msg)
      }
  }

}
