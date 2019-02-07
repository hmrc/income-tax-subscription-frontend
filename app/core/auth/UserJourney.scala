/*
 * Copyright 2019 HM Revenue & Customs
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

package core.auth

import core.auth.AuthPredicate._
import core.config.AppConfig
import core.config.featureswitch.FeatureSwitch

trait UserJourney[User <: IncomeTaxUser] {
  final implicit lazy val instance: this.type = this

  final def isEnabled(implicit appConfig: AppConfig): Boolean = featureSwitch.fold(true)(appConfig.isEnabled)

  val featureSwitch: Option[FeatureSwitch] = None

  def authPredicates(implicit appConfig: AppConfig): AuthPredicate[User]
}

