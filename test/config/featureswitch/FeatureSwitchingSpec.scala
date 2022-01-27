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

package config.featureswitch

import auth.agent.ConfirmAgentSubscription.featureSwitch
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class FeatureSwitchingSpec extends WordSpecLike with Matchers with OptionValues with FeatureSwitching {

  FeatureSwitch.switches foreach { switch =>
    s"isEnabled(${switch.name})" should {
      "return true when a feature switch is set" in {
        enable(switch)
        isEnabled(switch) shouldBe true
      }

      "return false when a feature switch is set to false" in {
        disable(switch)
        isEnabled(switch) shouldBe false
      }

      "return false when a feature switch has not been set" in {
        sys.props -= switch.name
        sys.props.get(switch.name) shouldBe empty
        isEnabled(switch) shouldBe false
      }
    }
  }

}

trait FeatureSwitchingUtil extends FeatureSwitching {
  def withFeatureSwitch(featureSwitch: FeatureSwitch)(f: => Any): Any = {
    enable (featureSwitch)
    try {
      f
    } finally {
      disable (featureSwitch)
    }
  }
}