/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ThrottlingService @Inject()(throttlingConnector: ThrottlingConnector,
                                  val appConfig: AppConfig) extends Logging with FeatureSwitching {

  def throttled(throttle: Throttle)(success: => Future[Result])
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    val throttleResult: Future[Boolean] = {
      if (isEnabled(ThrottlingFeature)) {
        throttlingConnector.getThrottleStatus(throttle.throttleId)
          .recoverWith { case _ =>
            logger.warn(s"Throttle ${throttle.throttleId} has failed, recovering with open=${throttle.failOpen}")
            Future.successful(throttle.failOpen)
          }
      } else {
        Future.successful(true)
      }
    }

    throttleResult flatMap {
      case true => success
      case false => Future.successful(Redirect(throttle.callOnFail))
    }

  }
}

trait Throttle {
  def throttleId: String

  /** Allow the request if the back end throttle service fails */
  def failOpen: Boolean

  def callOnFail: Call
}

object IndividualEndOfJourneyThrottle extends Throttle {
  override val throttleId: String = "end-of-journey"
  override val failOpen: Boolean = false
  override val callOnFail: Call = controllers.individual.routes.ThrottlingController.show
}
