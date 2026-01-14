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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utilities.UnitTestTrait

import java.time.LocalDate

class FeatureSwitchingSpec extends UnitTestTrait with BeforeAndAfterEach {

  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  val mockConfig: Configuration = mock[Configuration]
  val featureSwitching: FeatureSwitching = new FeatureSwitching {
    override val appConfig: FrontendAppConfig = new FrontendAppConfig(servicesConfig, mockConfig)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConfig)
    FeatureSwitch.switches foreach { switch =>
      sys.props -= switch.name
    }
  }

  "FeatureSwitching constants" should {
    "be true and false" in {
      FEATURE_SWITCH_ON mustBe "true"
      FEATURE_SWITCH_OFF mustBe "false"
    }
  }

  "Throttle" should {
    "return true if Throttle feature switch is enabled in sys.props" in {
      enable(ThrottlingFeature)
      isEnabled(ThrottlingFeature) mustBe true
    }
    "return false if Throttle feature switch is disabled in sys.props" in {
      disable(ThrottlingFeature)
      isEnabled(ThrottlingFeature) mustBe false
    }

    "return false if Throttle feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(None)
      isEnabled(ThrottlingFeature) mustBe false
    }

    "return false if Throttle feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_OFF))
      isEnabled(ThrottlingFeature) mustBe false
    }

    "return true if Throttle feature switch is not in sys.props but is set to on in config" in {
      enable(ThrottlingFeature)
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_ON))
      isEnabled(ThrottlingFeature) mustBe true
    }
  }

  "Auto toggle" should {

    case object TestFeature1 extends FeatureSwitch {
      override val name: String = "test.feature1"
      override val displayText: String = "Test feature switch one"
    }

    case object TestFeature2 extends FeatureSwitch {
      override val name: String = "test.feature2"
      override val displayText: String = "Test feature switch two"
    }

    case object TestFeature3 extends FeatureSwitch {
      override val name: String = "test.feature3"
      override val displayText: String = "Test feature switch three"
    }

    case object TestFeature4 extends FeatureSwitch {
      override val name: String = "test.feature4"
      override val displayText: String = "Test feature switch four"
    }

    case object TestFeature5 extends FeatureSwitch {
      override val name: String = "test.feature5"
      override val displayText: String = "Test feature switch five"
    }

    val allSwitches: Set[FeatureSwitch] = Set(
      TestFeature1,
      TestFeature2,
      TestFeature3,
      TestFeature4,
      TestFeature5
    )

    val now = LocalDate.now

    // This one is set to toggle in the past
    // so will be disabled until parsed
    val date1 = now.minusDays(1)
    when(mockConfig.getOptional[String](TestFeature1.name)).thenReturn(
      Some(date1.toString)
    )

    // This one is set to toggle in the future
    // so will be disabled until parsed
    val date2 = now.plusDays(1)
    when(mockConfig.getOptional[String](TestFeature2.name)).thenReturn(
      Some(date2.toString)
    )

    // This one is not set so assumed to be disabled
    when(mockConfig.getOptional[String](TestFeature3.name)).thenReturn(
      None
    )

    // This one is set to be enabled
    when(mockConfig.getOptional[String](TestFeature4.name)).thenReturn(
      Some(FEATURE_SWITCH_ON)
    )

    // This one is set to be disabled
    when(mockConfig.getOptional[String](TestFeature5.name)).thenReturn(
      Some(FEATURE_SWITCH_OFF)
    )

    val before = Map(allSwitches.toSeq.map {s => (s, featureSwitching.isEnabled(s))}: _*)
    val autoToggleSwitches = featureSwitching.init(allSwitches)
    val after = Map(allSwitches.toSeq.map {s => (s, featureSwitching.isEnabled(s))}: _*)

    // Those are parsed into memory so config is used
    // -  for initial checking
    // -  when parsing occurs
    // Memory is used for final checking
    verify(mockConfig, times(2)).getOptional[String](TestFeature1.name)
    verify(mockConfig, times(2)).getOptional[String](TestFeature2.name)

    // Those are never parsed into memory so config is used all the time
    verify(mockConfig, times(3)).getOptional[String](TestFeature3.name)
    verify(mockConfig, times(3)).getOptional[String](TestFeature4.name)
    verify(mockConfig, times(3)).getOptional[String](TestFeature5.name)

    def test(featureSwitch: FeatureSwitch, stateBefore: Boolean, stateAfter: Boolean): Unit = {
      before.get(featureSwitch) mustBe Some(stateBefore)
      after.get(featureSwitch) mustBe Some(stateAfter)
    }

    "Set switches that have a date in config to auto-toggle" in {
      autoToggleSwitches mustBe Map(
        TestFeature1 -> date1,
        TestFeature2 -> date2
      )
    }

    "Toggle switch set to toggle in the past" in {
      test(
        featureSwitch = TestFeature1,
        stateBefore = false,
        stateAfter = true
      )
    }

    "Leave switch set to toggle in the future alone" in {
      test(
        featureSwitch = TestFeature2,
        stateBefore = false,
        stateAfter = false
      )
    }

    "Ignore switch not present in config" in {
      test(
        featureSwitch = TestFeature3,
        stateBefore = false,
        stateAfter = false
      )
    }

    "Ignore switch set in config to be on" in {
      test(
        featureSwitch = TestFeature4,
        stateBefore = true,
        stateAfter = true
      )
    }

    "Ignore switch set ib config to be off" in {
      test(
        featureSwitch = TestFeature5,
        stateBefore = false,
        stateAfter = false
      )
    }
  }

}

trait FeatureSwitchingUtil extends FeatureSwitching {

  def withFeatureSwitch(featureSwitch: FeatureSwitch)(f: => Any): Any = {
    enable(featureSwitch)
    try {
      f
    } finally {
      disable(featureSwitch)
    }
  }

}
