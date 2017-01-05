/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import scala.concurrent.Future
import views.html.ivFailure._
import connectors.IdentityVerificationConnector
import enums.IdentityVerificationResult
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object UnauthorisedController extends UnauthorisedController {
	override val identityVerificationConnector: IdentityVerificationConnector = IdentityVerificationConnector
}

trait UnauthorisedController extends FrontendController {

  val identityVerificationConnector: IdentityVerificationConnector

  def showNotAuthorised(journeyId: Option[String]): Action[AnyContent] = UnauthorisedAction.async { implicit request =>
    val result = journeyId map { id =>
      val identityVerificationResult = identityVerificationConnector.identityVerificationResponse(id)
      identityVerificationResult map {
        case IdentityVerificationResult.FailedMatching => unauthorised()
        case IdentityVerificationResult.InsufficientEvidence => unauthorised()
        case IdentityVerificationResult.TechnicalIssue => technicalIssue()
        case IdentityVerificationResult.LockedOut => lockedOut()
        case IdentityVerificationResult.Timeout => views.html.timeout.timeout()
        case IdentityVerificationResult.Incomplete => unauthorised()
        case IdentityVerificationResult.PreconditionFailed => unauthorised()
        case IdentityVerificationResult.UserAborted => unauthorised()
        case IdentityVerificationResult.FailedIV => unauthorised()
        case IdentityVerificationResult.UnknownOutcome => unauthorised()
      }
    } getOrElse Future.successful(unauthorised()) // 2FA returns no journeyId

    result.map {
      Ok(_).withNewSession
    }
  }
}
