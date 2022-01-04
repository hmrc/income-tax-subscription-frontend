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

package auth.agent

import auth.individual.AuthPredicate._
import auth.individual.AuthPredicates._
import auth.individual.{IncomeTaxSAUser, JourneyState, UserJourney}
import cats.implicits._
import config.AppConfig
import config.featureswitch.FeatureSwitch

object ConfirmAgentSubscription extends UserJourney[IncomeTaxSAUser] with JourneyState {
  override val name: String = "confirmAgentSubscription"

  override val featureSwitch: Option[FeatureSwitch] = None

  override def authPredicates(implicit appConfig: AppConfig): AuthPredicate[IncomeTaxSAUser] =
    defaultPredicates |+| journeyStatePredicate |+| notEnrolledPredicate
}
