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

package controllers.agent.resolvers

import common.Constants.hmrcAsAgent
import config.AppConfig
import config.featureswitch.FeatureSwitch.OptBackIn
import config.featureswitch.FeatureSwitching
import models.requests.agent.IdentifierRequest
import models.status.GetITSAStatus
import models.status.GetITSAStatus.Annual
import models.{Channel, HmrcLedUnconfirmed, SessionData}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import services.GetITSAStatusService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlreadySignedUpResolver @Inject()(getITSAStatusService: GetITSAStatusService,
                                        val appConfig: AppConfig
                                       )(implicit ec: ExecutionContext) extends FeatureSwitching {

  def resolve(sessionData: SessionData, channel: Option[Channel])
             (implicit hc: HeaderCarrier, request: IdentifierRequest[AnyContent]): Future[Result] = {
    goToAlreadySignedUp(sessionData, channel).map(_.addingToSession(hmrcAsAgent -> "true"))
  }

  private def goToAlreadySignedUp(session: SessionData, channel: Option[Channel])(implicit hc: HeaderCarrier): Future[Result] =
    channel match {
      case Some(HmrcLedUnconfirmed) =>
        Future.failed(
          new InternalServerException("AlreadySignedUpResolver - Agent - HOA06A - Client migrated by HMRC")
        )
      case _ =>
        getITSAStatus(session).map {
          case Some(Annual) => Redirect(controllers.agent.handoffs.routes.OptedOutController.show)
          case _ => Redirect(controllers.agent.matching.routes.ClientAlreadySubscribedController.show)
        }
    }

  private def getITSAStatus(sessionData: SessionData)(implicit hc: HeaderCarrier): Future[Option[GetITSAStatus]] =
    if (isEnabled(OptBackIn)) {
      getITSAStatusService.getITSAStatus(sessionData).map(r => Some(r.status))
    } else {
      Future.successful(None)
    }
}