/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.UsingSoftwareForm.usingSoftwareForm
import models.{No, Yes}
import play.api.mvc._
import services.{GetEligibilityStatusService, MandationStatusService, SessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.UsingSoftware

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UsingSoftwareController @Inject()(view: UsingSoftware,
                                        identify: IdentifierAction,
                                        journeyRefiner: ConfirmedClientJourneyRefiner,
                                        sessionDataService: SessionDataService,
                                        eligibilityStatusService: GetEligibilityStatusService,
                                        mandationStatusService: MandationStatusService)
                                       (val appConfig: AppConfig)
                                       (implicit ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends SignUpBaseController {

  val show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    for {
      usingSoftwareStatus <- sessionDataService.fetchSoftwareStatus
      eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
    } yield {
      usingSoftwareStatus match {
        case Left(_) => throw new InternalServerException("[UsingSoftwareController][show] - Could not fetch software status")
        case Right(maybeYesNo) => Ok(view(
          usingSoftwareForm = usingSoftwareForm.fill(maybeYesNo),
          postAction = routes.UsingSoftwareController.submit,
          clientName = request.clientDetails.name,
          clientNino = request.clientDetails.formattedNino,
          backUrl = backUrl(eligibilityStatus.eligibleNextYearOnly)
        ))
      }
    }
  }

  val submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    usingSoftwareForm.bindFromRequest().fold(
      formWithErrors =>
        eligibilityStatusService.getEligibilityStatus map { eligibilityStatus =>
          BadRequest(view(
            usingSoftwareForm = formWithErrors,
            postAction = routes.UsingSoftwareController.submit,
            clientName = request.clientDetails.name,
            clientNino = request.clientDetails.formattedNino,
            backUrl = backUrl(eligibilityStatus.eligibleNextYearOnly)
          ))
        },
      yesNo =>
        for {
          usingSoftwareStatus <- sessionDataService.saveSoftwareStatus(yesNo)
          eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
          mandationStatus <- mandationStatusService.getMandationStatus
        } yield {

          val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
          val isEligibleNextYearOnly: Boolean = eligibilityStatus.eligibleNextYearOnly

          usingSoftwareStatus match {
            case Left(_) =>
              throw new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer")
            case Right(_) =>
              yesNo match {
                case Yes =>
                    if (isMandatedCurrentYear || isEligibleNextYearOnly) {
                      Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
                    } else {
                      Redirect(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show())
                    }
                case No =>
                  Redirect(controllers.agent.routes.NoSoftwareController.show())
              }
          }
        }
    )
  }

  def backUrl(eligibleNextYearOnly: Boolean): String = {
    if (eligibleNextYearOnly) {
      controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url
    } else {
      controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
    }
  }

}
