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

package config.featureswitch

import config.AppConfig
import play.api.Logging

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.util.Try

trait FeatureSwitching extends Logging {

  val appConfig: AppConfig

  val FEATURE_SWITCH_ON = "true"
  val FEATURE_SWITCH_OFF = "false"

  def isEnabled(featureSwitch: FeatureSwitch): Boolean = {
    lazy val systemProperty = sys.props.get(featureSwitch.name)
    lazy val configProperty = appConfig.configuration.getOptional[String](featureSwitch.name)
    lazy val configEnabled = featureSwitch match {
      case switch: DatedFeatureSwitch =>
        Try {
          (systemProperty orElse configProperty).map(LocalDate.parse) match {
            case Some(date) => !LocalDate.now().isBefore(date)
            case None =>
              logger.error(s"[FeatureSwitching][isEnabled] - No date specified for ${featureSwitch.name}")
              false
          }
        } getOrElse {
          logger.error(s"[FeatureSwitching][isEnabled] - Failed to parse date for ${featureSwitch.name}")
          false
        }
      case _ => configProperty contains FEATURE_SWITCH_ON
    }

    systemProperty match {
      case Some(FEATURE_SWITCH_ON) => true
      case Some(FEATURE_SWITCH_OFF) => false
      case _ => configEnabled
    }
  }

  def isDisabled(featureSwitch: FeatureSwitch): Boolean =
    !isEnabled(featureSwitch)

  def enable(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  def disable(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF
}

@Singleton
class FeatureSwitchingImpl @Inject()(val appConfig: AppConfig) extends FeatureSwitching
