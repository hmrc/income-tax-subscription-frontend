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
import models.{SessionData, YesNo}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.UserMatchingSessionResultUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionClearingService @Inject()(sessionDataService: SessionDataService)
                                      (implicit ec: ExecutionContext) {

  def clearAgentSession(nextPage: Call, sessionData: SessionData = SessionData())(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    for {
      emailConsentCaptured <- fetchEmailConsentCaptured(sessionData)
      _ <- deleteSessionData
      _ <- saveEmailConsentCaptured(emailConsentCaptured)
    } yield {
      Redirect(nextPage)
        .addingToSession(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
        .removingFromSession(MTDITID, CLIENT_DETAILS_CONFIRMED)
        .clearAllUserDetails(request)
    }
  }

  private def fetchEmailConsentCaptured(sessionData: SessionData): Future[Boolean] = {
    fetchEmailPassed(sessionData).flatMap {
      case Some(_) => Future.successful(true)
      case None => fetchConsentStatus(sessionData).map(_.isDefined)
    }
  }

  private def fetchEmailPassed(sessionData: SessionData): Future[Option[Boolean]] = {
    Future.successful(sessionData.fetchEmailPassed)
  }

  private def fetchConsentStatus(sessionData: SessionData): Future[Option[YesNo]] = {
    Future.successful(sessionData.fetchConsentStatus)
  }

  private def deleteSessionData(implicit hc: HeaderCarrier): Future[DeleteSessionDataSuccess] = {
    sessionDataService.deleteSessionAll map {
      case Left(error) => throw new InternalServerException(s"[SessionClearingService][deleteSessionData] - Unexpected failure: $error")
      case Right(result) => result
    }
  }

  private def saveEmailConsentCaptured(emailConsentCaptured: Boolean)
                                      (implicit hc: HeaderCarrier): Future[SaveSessionDataSuccess] = {
    if (emailConsentCaptured) {
      sessionDataService.saveEmailPassed(emailPassed = true) map {
        case Left(error) => throw new InternalServerException(s"[SessionClearingService][saveEmailConsentCaptured] - Unexpected failure: $error")
        case Right(result) =>
          result
      }
    } else {
      Future.successful(SaveSessionDataSuccessResponse)
    }
  }
}
