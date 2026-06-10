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
import config.featureswitch.FeatureSwitch.{TaxYear27To28Plus, ThrottlingFeature, CompositeEnrolmentKey, DistributedKnownFactsPattern}
import org.mockito.Mockito.{reset, when}
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

  "TaxYear27To28Plus" should {
    "return true" when {
      "the feature switch is enabled in system properties" in {
        enable(TaxYear27To28Plus)
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
      "the date in the system properties is before the current date" in {
        sys.props += TaxYear27To28Plus.name -> LocalDate.now.minusDays(1).toString
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
      "the date in the system properties is the current date" in {
        sys.props += TaxYear27To28Plus.name -> LocalDate.now.toString
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
      "the date in the config is before the current date" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(Some(LocalDate.now.minusDays(1).toString))
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
      "the date in the config is the current date" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(Some(LocalDate.now.toString))
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
      "the date in the config is after the current date, but later the system property for this feature is set to true" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(Some(LocalDate.now.plusDays(1).toString))
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false

        enable(TaxYear27To28Plus)
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true
      }
    }
    "return false" when {
      "the feature switch is disabled in system properties" in {
        disable(TaxYear27To28Plus)
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
      "the feature switch does not exist in config or system properties" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(None)
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
      "the date in the system properties is after current date" in {
        sys.props += TaxYear27To28Plus.name -> LocalDate.now.plusDays(1).toString
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
      "the date in the system properties could not be parsed" in {
        sys.props += TaxYear27To28Plus.name -> "invalid-date"
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
      "the date in the config is after the current date" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(Some(LocalDate.now.plusDays(1).toString))
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
      "the date in the config is before the current date, but later the system property for this feature is set to true" in {
        when(mockConfig.getOptional[String]("feature-switch.tax-year-27-28-plus")).thenReturn(Some(LocalDate.now.minusDays(1).toString))
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe true

        disable(TaxYear27To28Plus)
        featureSwitching.isEnabled(TaxYear27To28Plus) mustBe false
      }
    }
  }

  "Throttle" should {
    "return true if Throttle feature switch is enabled in sys.props" in {
      enable(ThrottlingFeature)
      featureSwitching.isEnabled(ThrottlingFeature) mustBe true
    }
    "return false if Throttle feature switch is disabled in sys.props" in {
      disable(ThrottlingFeature)
      featureSwitching.isEnabled(ThrottlingFeature) mustBe false
    }

    "return false if Throttle feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(None)
      featureSwitching.isEnabled(ThrottlingFeature) mustBe false
    }

    "return false if Throttle feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_OFF))
      featureSwitching.isEnabled(ThrottlingFeature) mustBe false
    }

    "return true if Throttle feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.throttle")).thenReturn(Some(FEATURE_SWITCH_ON))
      featureSwitching.isEnabled(ThrottlingFeature) mustBe true
    }
  }

  "CompositeEnrolmentKey" should {
    "return true if CompositeEnrolmentKey feature switch is enabled in sys.props" in {
      enable(CompositeEnrolmentKey)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe true
    }

    "return false if CompositeEnrolmentKey feature switch is disabled in sys.props" in {
      disable(CompositeEnrolmentKey)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return false if CompositeEnrolmentKey feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(None)
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return false if CompositeEnrolmentKey feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(Some(FEATURE_SWITCH_OFF))
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }

    "return false if CompositeEnrolmentKey feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.composite-enrolment-key")).thenReturn(Some(FEATURE_SWITCH_ON))
      featureSwitching.isEnabled(CompositeEnrolmentKey) mustBe false
    }
  }

  "DistributedKnownFactsPattern" should {
    "return true if DistributedKnownFactsPattern feature switch is enabled in sys.props" in {
      enable(DistributedKnownFactsPattern)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe true
    }

    "return false if DistributedKnownFactsPattern feature switch is disabled in sys.props" in {
      disable(DistributedKnownFactsPattern)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return false if DistributedKnownFactsPattern feature switch does not exist" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(None)
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return false if DistributedKnownFactsPattern feature switch is not in sys.props but is set to off in config" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(Some(FEATURE_SWITCH_OFF))
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
    }

    "return false if DistributedKnownFactsPattern feature switch is not in sys.props but is set to on in config" in {
      when(mockConfig.getOptional[String]("feature-switch.distributed-known-facts-pattern")).thenReturn(Some(FEATURE_SWITCH_ON))
      featureSwitching.isEnabled(DistributedKnownFactsPattern) mustBe false
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
