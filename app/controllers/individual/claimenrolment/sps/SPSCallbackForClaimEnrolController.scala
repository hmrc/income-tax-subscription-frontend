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

package controllers.individual.claimenrolment.sps

import auth.individual.BaseClaimEnrolmentController
import common.Constants.ITSASessionKeys
import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.individual.claimenrolment.ClaimEnrolmentService
import services.{AuditingService, AuthService, SPSService}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SPSCallbackForClaimEnrolController @Inject()(val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   spsService: SPSService,
                                                   claimEnrolmentService: ClaimEnrolmentService)
                                                  (implicit val appConfig: AppConfig,
                                                   val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends BaseClaimEnrolmentController {

  def callback: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      request.queryString.get("entityId").flatMap(_.headOption) match {
        case Some(entityId) =>
          claimEnrolmentService.getMtditidFromSubscription flatMap {
            case Right(mtdItId) =>
              spsService.confirmPreferences(mtdItId, Some(entityId)) map { _ =>
                Redirect(controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.show()).addingToSession(
                  ITSASessionKeys.SPSEntityId -> entityId
                )
              }
            case Left(_) => throw new InternalServerException(
              "[SPSCallbackForClaimEnrolController][callback] - failed to retrieve mtditid from claimEnrolmentService"
            )
          }
        case None => Future.successful(Redirect(controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.show()))
      }
  }
}
