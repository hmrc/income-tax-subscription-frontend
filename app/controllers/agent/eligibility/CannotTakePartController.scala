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

package controllers.agent.eligibility

import controllers.SignUpBaseController
import controllers.agent.actions.{IdentifierAction, SignPostedJourneyRefiner}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.agent.eligibility.CannotTakePart
import services.GetEligibilityStatusService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CannotTakePartController @Inject()(view: CannotTakePart,
                                         identify: IdentifierAction,
                                         getEligibilityStatusService: GetEligibilityStatusService,
                                         journeyRefiner: SignPostedJourneyRefiner)
                                        (implicit cc: MessagesControllerComponents,  ec: ExecutionContext) extends SignUpBaseController {

  val show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      eligibilityStatus <- getEligibilityStatusService.getEligibilityStatus
    } yield {
      val maybeReason = eligibilityStatus.exceptionReason.getOrElse("unknown")

      Ok(view(
        clientName = request.clientDetails.name,
        clientNino = request.clientDetails.formattedNino,
        exemptionReason = maybeReason
      )).addingToSession(
        "exemptionReason" -> maybeReason
      )
      }
    }
}
