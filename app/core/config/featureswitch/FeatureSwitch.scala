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

package core.config.featureswitch

import core.config.featureswitch.FeatureSwitch.prefix

sealed trait FeatureSwitch {
  val name: String
  val displayText: String
}

object FeatureSwitch {
  val prefix = "feature-switch"

  val switches: Set[FeatureSwitch]  = Set(
    RegistrationFeature,
    UnauthorisedAgentFeature,
    WelshLanguageFeature,
    EligibilityPagesFeature,
    UseSubscriptionApiV2,
    PropertyCashOrAccruals,
    WhatTaxYearToSignUp
  )

  def apply(str: String): FeatureSwitch =
    switches find (_.name == str) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  def get(str: String): Option[FeatureSwitch] = switches find (_.name == str)

}

case object RegistrationFeature extends FeatureSwitch {
  override val name = s"$prefix.enable-registration"
  override val displayText = "Registration journey"
}

case object UnauthorisedAgentFeature extends FeatureSwitch {
  override val name = s"$prefix.enable-unauthorised-agent"
  override val displayText = "Unauthorised agent journey"
}

case object WelshLanguageFeature extends FeatureSwitch {
  override val name = s"$prefix.welsh-translation"
  override val displayText = "Enable welsh language"
}

case object EligibilityPagesFeature extends FeatureSwitch {
  override val name = s"$prefix.eligibility-pages"
  override val displayText = "Remove terms and do you have other income pages from end of flow"
}

case object UnplannedShutter extends FeatureSwitch {
  override val name: String = s"$prefix.unplanned-shutter"
  override val displayText: String = "Unplanned shutter for the service"
}

case object UseSubscriptionApiV2 extends FeatureSwitch {
  override val displayText: String = "Use version 2 of the subscription API"
  override val name: String = s"$prefix.use-subscription-api-v2"
}

case object PropertyCashOrAccruals extends FeatureSwitch {
  override val displayText: String = "Enable cash or accruals choice for property income"
  override val name: String = s"$prefix.property-cash-or-accruals"
}

case object WhatTaxYearToSignUp extends FeatureSwitch {
  override val displayText: String = "Enable what tax year do you want to sign up for"
  override val name: String = s"$prefix.what-tax-year-to-sign-up"
}