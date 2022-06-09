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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitch._
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utilities.UnitTestTrait

class FeatureSwitchingSpec extends UnitTestTrait with BeforeAndAfterEach {

  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  val mockConfig: Configuration = mock[Configuration]
  override val appConfig: FrontendAppConfig = new FrontendAppConfig(servicesConfig, mockConfig)

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

  "ForeignProperty" should {
    "return true if ForeignProperty feature switch is enabled in sys.props" in {
      enable(ForeignProperty)
      isEnabled(ForeignProperty) mustBe true
    }
    "return false if ForeignProperty feature switch is disabled in sys.props" in {
      disable(ForeignProperty)
      isEnabled(ForeignProperty) mustBe false
    }

    "return false if ForeignProperty feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-foreign-property")).thenReturn(None)
      isEnabled(ForeignProperty) mustBe false
    }

    "return false if ForeignProperty feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-foreign-property")).thenReturn(Some(FEATURE_SWITCH_OFF))
      isEnabled(ForeignProperty) mustBe false
    }

    "return true if ForeignProperty feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-foreign-property")).thenReturn(Some(FEATURE_SWITCH_ON))
      isEnabled(ForeignProperty) mustBe true
    }
  }

  "SaveAndRetrieve" should {
    "return true if SaveAndRetrieve feature switch is enabled in sys.props" in {
      enable(SaveAndRetrieve)
      isEnabled(SaveAndRetrieve) mustBe true
    }
    "return false if SaveAndRetrieve feature switch is disabled in sys.props" in {
      disable(SaveAndRetrieve)
      isEnabled(SaveAndRetrieve) mustBe false
    }

    "return false if SaveAndRetrieve feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-save-and-retrieve")).thenReturn(None)
      isEnabled(SaveAndRetrieve) mustBe false
    }

    "return false if SaveAndRetrieve feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-save-and-retrieve")).thenReturn(Some(FEATURE_SWITCH_OFF))
      isEnabled(SaveAndRetrieve) mustBe false
    }

    "return true if SaveAndRetrieve feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.enable-save-and-retrieve")).thenReturn(Some(FEATURE_SWITCH_ON))
      isEnabled(SaveAndRetrieve) mustBe true
    }
  }

  "PrePopulate" should {
    "return true if PrePopulate feature switch is enabled in sys.props" in {
      enable(PrePopulate)
      isEnabled(PrePopulate) mustBe true
    }
    "return false if PrePopulate feature switch is disabled in sys.props" in {
      disable(PrePopulate)
      isEnabled(PrePopulate) mustBe false
    }

    "return false if PrePopulate feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.prepopulate")).thenReturn(None)
      isEnabled(PrePopulate) mustBe false
    }

    "return false if PrePopulate feature switch is not in sys.props but is set to off in config" in {

      when(mockConfig.getOptional[String]("feature-switch.prepopulate")).thenReturn(Some(FEATURE_SWITCH_OFF))
      isEnabled(PrePopulate) mustBe false
    }

    "return true if PrePopulate feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.prepopulate")).thenReturn(Some(FEATURE_SWITCH_ON))
      isEnabled(PrePopulate) mustBe true
    }
  }

  "Throttle" should {
    "return true if Throttle feature switch is enabled in sys.props" in {
      enable(Throttle)
      isEnabled(Throttle) mustBe true
    }
    "return false if Throttle feature switch is disabled in sys.props" in {
      disable(Throttle)
      isEnabled(Throttle) mustBe false
    }

    "return false if Throttle feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(None)
      isEnabled(Throttle) mustBe false
    }

    "return false if Throttle feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_OFF))
      isEnabled(Throttle) mustBe false
    }

    "return true if Throttle feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_ON))
      isEnabled(Throttle) mustBe true
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
