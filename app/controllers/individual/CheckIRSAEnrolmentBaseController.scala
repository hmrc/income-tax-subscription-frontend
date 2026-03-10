/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.individual

import cats.data.EitherT
import config.AppConfig
import connectors.UsersGroupsSearchConnector
import connectors.agent.EnrolmentStoreProxyConnector
import connectors.agent.httpparsers.QueryUsersHttpParser.UsersFound
import controllers.SignUpBaseController
import controllers.individual.actions.IdentifierAction
import forms.individual.IRSACredentialForm
import models.individual.ObfuscatedIdentifier
import models.requests.individual.IdentifierRequest
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.UTRService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.individual.IRSACredential

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class CheckIRSAEnrolmentBaseController(
  utrService: UTRService,
  usersGroupsSearchConnector: UsersGroupsSearchConnector,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  protected final def getIdentifierDetails(
    implicit request: IdentifierRequest[_], hc: HeaderCarrier): Future[Either[IdentifiersFailure.type, IdentifierDetails]
  ] = {
    utrService.getUTR(request.sessionData) flatMap { utr =>

      val obfuscatedIdentifierResult = for {
        saCredId <- EitherT(getSACredId(currentCredId = request.credentials.providerId, utr = utr))
        saCredObfuscatedIdentifier <- EitherT(getUserDetailsByCredId(credId = saCredId))
        currentCredObfuscatedIdentifier <- EitherT(getUserDetailsByCredId(credId = request.credentials.providerId))
      } yield {
        IdentifierDetails(
          currentCredential = currentCredObfuscatedIdentifier,
          saCredential = saCredObfuscatedIdentifier
        )
      }

      obfuscatedIdentifierResult.value
    }
  }

  private def getSACredId(currentCredId: String, utr: String)(implicit hc: HeaderCarrier): Future[Either[IdentifiersFailure.type, String]] = {
    enrolmentStoreProxyConnector.getUserIds(utr) map {
      case Right(UsersFound(saCredIds)) if saCredIds.nonEmpty && !saCredIds.contains(currentCredId) =>
        Right(saCredIds.head)
      case _ => Left(IdentifiersFailure)
    }
  }

  protected final def getUserDetailsByCredId(credId: String)(implicit hc: HeaderCarrier): Future[Either[IdentifiersFailure.type, ObfuscatedIdentifier]] = {
    usersGroupsSearchConnector.getUserDetailsByCredId(credId) map {
      case Right(obfuscatedIdentifier) => Right(obfuscatedIdentifier)
      case _ => Left(IdentifiersFailure)
    }
  }

  protected final case class IdentifierDetails(currentCredential: ObfuscatedIdentifier, saCredential: ObfuscatedIdentifier)

  protected case object IdentifiersFailure

}
