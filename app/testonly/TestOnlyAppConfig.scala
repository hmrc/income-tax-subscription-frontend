/*
 * Copyright 2020 HM Revenue & Customs
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

//$COVERAGE-OFF$Disabling scoverage on this test only class as it is only required by our acceptance test

package testonly

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class TestOnlyAppConfig @Inject()(config: ServicesConfig) extends FrontendAppConfig(config: ServicesConfig){

  override lazy val ggAuthenticationURL: String = config.baseUrl("gg-authentication")

  lazy val entityResolverURL: String = config.baseUrl("entity-resolver")

  lazy val preferencesURL: String = config.baseUrl("preferences")

  lazy val protectedMicroServiceTestOnlyUrl = s"$microServiceUrl/income-tax-subscription/test-only"

  lazy val matchingStubsURL: String = config.baseUrl("matching-stubs")

  lazy val taxEnrolmentsURL: String = config.baseUrl("tax-enrolments")

  lazy val enrolmentStoreStubUrl: String = config.baseUrl("enrolment-store-stub")

}

// $COVERAGE-ON$
