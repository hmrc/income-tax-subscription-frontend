/*
 * Copyright 2017 HM Revenue & Customs
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

package config.featureswitch

import FeatureSwitch.prefix

sealed trait FeatureSwitch {
  val name: String
  val displayText: String
}

object FeatureSwitch {
  val prefix = "feature-switch"

  def apply(str: String): FeatureSwitch =
    switches.filter(_.name == str).toList match {
      case head :: Nil => head
      case _ => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  val switches = Set(UserMatching, NewPreferencesApi, Registration)
}

object UserMatching extends FeatureSwitch {
  val name = s"$prefix.user-matching"
  val displayText = "User matching"
}

object NewPreferencesApi extends FeatureSwitch {
  val name = s"$prefix.new-preferences-api"
  val displayText = "New preferences' API"
}

object Registration extends FeatureSwitch {
  val name = s"$prefix.enable-registration"
  val displayText = "Registration journey"
}
