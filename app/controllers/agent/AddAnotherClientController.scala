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

package controllers.agent

import auth.agent.{AgentUserMatching, AuthPredicates, IncomeTaxAgentUser, StatelessController}
import auth.individual.AuthPredicate.AuthPredicate
import cats.data.EitherT
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{ELIGIBLE_NEXT_YEAR_ONLY, MTDITID, NINO, UTR}
import config.AppConfig
import connectors.httpparser.DeleteSessionDataHttpParser
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionResultUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddAnotherClientController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val sessionDataService: SessionDataService,
                                           val appConfig: AppConfig)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends StatelessController {


  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      val sessionClearing: EitherT[Future, DeleteSessionDataHttpParser.DeleteSessionDataFailure, Result] = for {
        _ <- EitherT(sessionDataService.deleteThrottlePassed(AgentStartOfJourneyThrottle))
        _ <- EitherT(sessionDataService.deleteThrottlePassed(AgentEndOfJourneyThrottle))
        _ <- EitherT(sessionDataService.deleteMandationStatus)
        _ <- EitherT(sessionDataService.deleteReference)
      } yield {
        Redirect(controllers.agent.matching.routes.ClientDetailsController.show())
          .addingToSession(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
          .removingFromSession(MTDITID, NINO, UTR, ELIGIBLE_NEXT_YEAR_ONLY)
          .clearUserName
      }

      sessionClearing.value.map {
        case Left(error) => throw new InternalServerException(s"[AddAnotherClientController][addAnother] - Unexpected failure: $error")
        case Right(result) => result
      }
  }

}
