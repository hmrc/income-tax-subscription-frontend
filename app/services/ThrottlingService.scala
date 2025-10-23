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

package services

import config.AppConfig
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitching
import connectors.ThrottlingConnector
import models.SessionData
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ThrottlingService @Inject()(throttlingConnector: ThrottlingConnector,
                                  sessionDataService: SessionDataService,
                                  val appConfig: AppConfig) extends Logging with FeatureSwitching {

  def throttled(throttle: Throttle, sessionData: SessionData = SessionData())(success: => Future[Result])
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    if (isEnabled(ThrottlingFeature)) {
      sessionData.fetchThrottlePassed(throttle) match {
        case Some(_) =>
          success
        case None =>
          throttleResult(throttle) flatMap {
            case true =>
              sessionDataService.saveThrottlePassed(throttle) flatMap {
                case Right(_) =>
                  sessionData.saveThrottlePassed(throttle)
                  success
                case Left(_) =>
                  throw new InternalServerException(s"[ThrottlingService][throttled] - Unexpected failure when saving throttle pass: ${throttle.throttleId}")
              }
            case false =>
              Future.successful(Redirect(throttle.callOnFail))
          }
      }
    } else {
      success
    }
  }

  private[services] def throttleResult(throttle: Throttle)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    throttlingConnector.getThrottleStatus(throttle.throttleId)
      .recover {
        case _ =>
          logger.warn(s"Throttle ${throttle.throttleId} has failed, recovering with open=${throttle.failOpen}")
          throttle.failOpen
      }
  }
}

sealed trait Throttle {
  def throttleId: ThrottleId

  /** Allow the request if the back end throttle service fails */
  def failOpen: Boolean

  def callOnFail: Call
}

sealed abstract class ThrottleId(val name: String) {
  override def toString: String = name
}

case object StartOfJourneyThrottleId extends ThrottleId("start-of-journey")

case object IndividualStartOfJourneyThrottle extends Throttle {
  override val throttleId: ThrottleId = StartOfJourneyThrottleId
  override val failOpen: Boolean = false
  override val callOnFail: Call = controllers.individual.routes.ThrottlingController.start()
}

case object AgentStartOfJourneyThrottle extends Throttle {
  override val throttleId: ThrottleId = StartOfJourneyThrottleId
  override val failOpen: Boolean = false
  override val callOnFail: Call = controllers.agent.routes.ThrottlingController.start()
}
