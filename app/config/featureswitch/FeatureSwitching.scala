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
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import javax.inject.{Inject, Singleton}

trait FeatureSwitching {

  val appConfig: AppConfig

  val FEATURE_SWITCH_ON = "true"
  val FEATURE_SWITCH_OFF = "false"

  def isEnabled(featureSwitch: FeatureSwitch): Boolean =
    (sys.props.get(featureSwitch.name) orElse appConfig.configuration.getOptional[String](featureSwitch.name)) contains FEATURE_SWITCH_ON

  def isDisabled(featureSwitch: FeatureSwitch): Boolean =
    !isEnabled(featureSwitch)

  def enable(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  def disable(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF

  protected def autoToggle(autoToggleSwitches: Map[FeatureSwitch, LocalDate]): Map[FeatureSwitch, LocalDate] = {
    autoToggleSwitches.foreach { entry =>
      val state = !LocalDate.now.isBefore(entry._2)
      sys.props += entry._1.name -> state.toString
    }
    autoToggleSwitches
  }

  private def getAutoToggleDate(featureSwitch: FeatureSwitch): Option[LocalDate] = {
    appConfig.configuration.getOptional[String](featureSwitch.name).flatMap { value =>
      try {
        Some(LocalDate.parse(value))
      } catch {
        case e: Exception => None
      }
    }
  }

  def init(featureSwitches: Set[FeatureSwitch] = FeatureSwitch.switches): Map[FeatureSwitch, LocalDate] = {
    autoToggle(
      Map(featureSwitches.toSeq.map { s => (s, getAutoToggleDate(s)) }: _*)
        .filter(e => e._2.isDefined).map {e => (e._1, e._2.get)}
    )
  }
}

@Singleton
class FeatureSwitchingImpl @Inject()(val appConfig: AppConfig) extends FeatureSwitching with Runnable {
  private val autoToggleSwitches = init()
  if (autoToggleSwitches.nonEmpty) {
    new ScheduledThreadPoolExecutor(1)
      .scheduleAtFixedRate(this, 1, 1, TimeUnit.HOURS)
  }

  override def run(): Unit =
    autoToggle(autoToggleSwitches)
}
