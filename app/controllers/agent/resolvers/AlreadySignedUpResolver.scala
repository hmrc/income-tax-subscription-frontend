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

import config.AppConfig
import config.featureswitch.FeatureSwitching
import common.Constants.hmrcAsAgent
import config.featureswitch.FeatureSwitch.OptBackIn
import models.status.GetITSAStatus
import models.status.GetITSAStatus.Annual
import controllers.utils.ReferenceRetrieval
import models.requests.agent.IdentifierRequest
import models.{Channel, HmrcLedConfirmed, HmrcLedUnconfirmed, SessionData}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.{GetITSAStatusService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlreadySignedUpResolver @Inject()(getITSAStatusService: GetITSAStatusService,
                                         referenceRetrieval: ReferenceRetrieval,
                                         subscriptionDetailsService: SubscriptionDetailsService,
                                         val appConfig: AppConfig
                                       )(implicit ec: ExecutionContext)
  extends FeatureSwitching {

  def resolve(sessionData: SessionData, channel: Option[Channel])
             (implicit hc: HeaderCarrier, request: IdentifierRequest[AnyContent]): Future[Result] = {
    implicit val userArn: String = request.arn
    goToAlreadySignedUp(sessionData, channel).map(_.addingToSession(hmrcAsAgent -> "true"))
  }

  private def goToAlreadySignedUp(session: SessionData, channel: Option[Channel])
                                 (implicit hc: HeaderCarrier, userArn: String, request: Request[AnyContent]): Future[Result] = {

    val triggeredMigration = channel.exists {
      case HmrcLedUnconfirmed | HmrcLedConfirmed => true
      case _ => false
    }

    if (triggeredMigration) {
      for {
        reference <- referenceRetrieval.getAgentReference(session)
        confirmed <- subscriptionDetailsService.fetchIncomeSourcesConfirmation(reference)
        result <- if (!confirmed.contains(true)) {
          Future.failed(new InternalServerException("AlreadySignedUpResolver - Agent - HOA06A - Client migrated by HMRC"))
        } else {
          getITSAStatus(session).flatMap {
            case Some(Annual) => Future.failed(new InternalServerException("AlreadySignedUpResolver - Agent - HOA06B - Client opted out"))
            case _ => Future.successful(Redirect(controllers.agent.matching.routes.ClientAlreadySubscribedController.show))
          }
        }
      } yield result
    } else {
      getITSAStatus(session).flatMap {
        case Some(Annual) => Future.failed(new InternalServerException("AlreadySignedUpResolver - Agent - HOA06B - Client opted out"))
        case _ => Future.successful(Redirect(controllers.agent.matching.routes.ClientAlreadySubscribedController.show))
      }
    }
  }

  private def getITSAStatus(sessionData: SessionData)(implicit hc: HeaderCarrier): Future[Option[GetITSAStatus]] =
    if (isEnabled(OptBackIn)) {
      getITSAStatusService.getITSAStatus(sessionData).map(r => Some(r.status))
    } else {
      Future.successful(None)
    }
}