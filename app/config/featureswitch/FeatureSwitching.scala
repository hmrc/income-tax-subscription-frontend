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

trait FeatureSwitching extends Logging {

  val appConfig: AppConfig

  val FEATURE_SWITCH_ON = "true"
  val FEATURE_SWITCH_OFF = "false"

  def isEnabled(featureSwitch: FeatureSwitch): Boolean = {
    lazy val systemProperty = sys.props.get(featureSwitch.name)
    lazy val configEnabled = featureSwitch match {
      case switch: DatedFeatureSwitch =>
        appConfig.configuration.getOptional[String](featureSwitch.name).map(LocalDate.parse) match {
          case Some(date) =>
            logger.info(s"[FeatureSwitching][isEnabled] - Date: $date")
            !LocalDate.now().isBefore(date)
          case None =>
            logger.info(s"[FeatureSwitching][isEnabled] - Date not found")
            false
        }
      case _ => appConfig.configuration.getOptional[String](featureSwitch.name) contains FEATURE_SWITCH_ON
    }

    systemProperty match {
      case Some(value) => value contains FEATURE_SWITCH_ON
      case None => configEnabled
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
