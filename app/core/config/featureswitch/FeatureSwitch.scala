/*
 * Copyright 2018 HM Revenue & Customs
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

package core.config.featureswitch

import core.config.featureswitch.FeatureSwitch.prefix

sealed trait FeatureSwitch {
  val name: String
  val displayText: String
}

object FeatureSwitch {
  val prefix = "feature-switch"

  val switches = Set(
    RegistrationFeature,
    EmacEs6ApiFeature,
    EmacEs8ApiFeature,
    UnauthorisedAgentFeature,
    NewIncomeSourceFlowFeature
  )

  def apply(str: String): FeatureSwitch =
    switches find (_.name == str) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  def get(str: String): Option[FeatureSwitch] = switches find (_.name == str)

}

object RegistrationFeature extends FeatureSwitch {
  val name = s"$prefix.enable-registration"
  val displayText = "Registration journey"
}

object EmacEs6ApiFeature extends FeatureSwitch {
  val name = s"$prefix.enable-emac-es6"
  val displayText = "EMAC ES6 API (Upsert Enrolment)"
}

object EmacEs8ApiFeature extends FeatureSwitch {
  val name = s"$prefix.enable-emac-es8"
  val displayText = "EMAC ES8 API (Allocate enrolment)"
}

object UnauthorisedAgentFeature extends FeatureSwitch {
  val name = s"$prefix.enable-unauthorised-agent"
  val displayText = "Unauthorised agent journey"
}

object NewIncomeSourceFlowFeature extends FeatureSwitch {
  val name = s"$prefix.enable-new-income-source-flow"
  val displayText = "Enable new income source flow"
}