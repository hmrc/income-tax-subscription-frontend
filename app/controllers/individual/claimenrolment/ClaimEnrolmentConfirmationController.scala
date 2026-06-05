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

import config.AppConfig
import controllers.SignUpBaseController
import controllers.individual.actions.IdentifierAction
import models.individual.claimenrolment.ClaimEnrolmentOrigin
import models.individual.claimenrolment.ClaimEnrolmentOrigin.ClaimEnrolmentBTA
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionDataService
import views.html.individual.claimenrolment.ClaimEnrolmentConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ClaimEnrolmentConfirmationController @Inject()(identify: IdentifierAction,
                                                     sessionDataService: SessionDataService,
                                                     claimEnrolmentConfirmation: ClaimEnrolmentConfirmation)
                                                    (implicit val ec: ExecutionContext,
                                                     val appConfig: AppConfig,
                                                     mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = identify.async { implicit request =>
    sessionDataService.getAllSessionData().map { sessionData =>
      val origin: ClaimEnrolmentOrigin = sessionData.fetchClaimEnrolmentOrigin.getOrElse(ClaimEnrolmentBTA)
      Ok(claimEnrolmentConfirmation(
        postAction = controllers.individual.claimenrolment.routes.ClaimEnrolmentConfirmationController.submit(),
        origin = origin
      ))
    }
  }

  def submit: Action[AnyContent] = identify { _ =>
    Redirect(appConfig.getAccountUrl)
  }

}
