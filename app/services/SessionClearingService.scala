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

package services

import auth.agent.AgentUserMatching
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{CLIENT_DETAILS_CONFIRMED, MTDITID}
import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataSuccess
import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataSuccess, SaveSessionDataSuccessResponse}
import models.YesNo
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.UserMatchingSessionResultUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionClearingService @Inject()(sessionDataService: SessionDataService)
                                      (implicit ec: ExecutionContext) {

  def clearAgentSession(nextPage: Call)(implicit request: Request[AnyContent], hc: HeaderCarrier): Result = {
    for {
      emailConsentCaptured <- fetchEmailConsentCaptured
      _ <- deleteSessionData
      savedResult <- saveEmailConsentCaptured(emailConsentCaptured)
    } yield {
      savedResult == SaveSessionDataSuccessResponse
    }

    Redirect(nextPage)
      .addingToSession(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
      .removingFromSession(MTDITID, CLIENT_DETAILS_CONFIRMED)
      .clearAllUserDetails(request)
  }

  private def fetchEmailConsentCaptured(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    fetchEmailPassed flatMap {
      case Some(_) => Future.successful(true)
      case None => fetchConsentStatus.map(_.isDefined)
    }
  }

  private def fetchEmailPassed(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    sessionDataService.fetchEmailPassed map {
      case Left(error) => throw new InternalServerException(s"[AddAnotherClientController][fetchEmailPassed] - Unexpected failure: $error")
      case Right(result) => result
    }
  }

  private def fetchConsentStatus(implicit request: Request[_], hc: HeaderCarrier): Future[Option[YesNo]] = {
    sessionDataService.fetchConsentStatus map {
      case Left(error) => throw new InternalServerException(s"[AddAnotherClientController][fetchConsentStatus] - Unexpected failure: $error")
      case Right(result) => result
    }
  }

  private def deleteSessionData(implicit request: Request[_], hc: HeaderCarrier): Future[DeleteSessionDataSuccess] = {
    sessionDataService.deleteSessionAll map {
      case Left(error) => throw new InternalServerException(s"[AddAnotherClientController][deleteSessionData] - Unexpected failure: $error")
      case Right(result) => result
    }
  }

  private def saveEmailConsentCaptured(emailConsentCaptured: Boolean)
                                      (implicit request: Request[_], hc: HeaderCarrier): Future[SaveSessionDataSuccess] = {
    if (emailConsentCaptured) {
      sessionDataService.saveEmailPassed(emailPassed = true) map {
        case Left(error) => throw new InternalServerException(s"[AddAnotherClientController][saveEmailConsentCaptured] - Unexpected error: $error")
        case Right(result) => result
      }
    } else {
      Future.successful(SaveSessionDataSuccessResponse)
    }
  }


}