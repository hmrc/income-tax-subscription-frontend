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

package testonly.controllers

import connectors.usermatching.CitizenDetailsConnector
import models.usermatching.CitizenDetails
import play.api.mvc._
import services.SessionDataService
import testonly.connectors.ResetDataConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ResetDataController @Inject()(mcc: MessagesControllerComponents,
                                    citizenDetailsConnector: CitizenDetailsConnector,
                                    sessionDataService: SessionDataService,
                                    resetDataConnector: ResetDataConnector)
                                   (implicit ec: ExecutionContext) extends FrontendController(mcc) {

  def resetWithoutIdentifiers: Action[AnyContent] = Action.async { implicit request =>
    sessionDataService.fetchUTR flatMap {
      case Right(Some(utr)) => reset(utr)
      case Right(None) => Future.successful(Ok("No session data found"))
      case Left(_) => Future.successful(Ok("Error occurred when fetching utr from session, unable to reset data"))
    }
  }

  def resetWithUTR(utr: String): Action[AnyContent] = Action.async { implicit request =>
    reset(utr)
  }

  def resetWithNino(nino: String): Action[AnyContent] = Action.async { implicit request =>
    citizenDetailsConnector.lookupCitizenDetails(nino) flatMap {
      case Right(Some(CitizenDetails(Some(utr), _))) => reset(utr)
      case Right(_) => Future.successful(Ok("This nino has no associated utr, unable to reset data"))
      case Left(_) => Future.successful(Ok("Error occurred when looking up associated utr, unable to reset data"))
    }
  }

  private def reset(utr: String)(implicit request: Request[AnyContent]): Future[Result] = {
    resetDataConnector.reset(utr) flatMap {
      case true =>
        sessionDataService.deleteReference map {
          case Right(_) => Ok("Successfully reset data")
          case Left(_) => InternalServerError("Failed to remove reference from session")
        }
      case false => Future.successful(Ok("Error occurred when trying to reset data"))
    }
  }

}
