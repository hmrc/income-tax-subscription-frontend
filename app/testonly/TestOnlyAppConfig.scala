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

//$COVERAGE-OFF$Disabling scoverage on this test only class as it is only required by our acceptance test

package testonly

import javax.inject.{Inject, Singleton}

import core.config.FrontendAppConfig
import play.api.{Configuration, Environment}

@Singleton
class TestOnlyAppConfig @Inject()(configuration: Configuration,
                                  environment: Environment) extends FrontendAppConfig(configuration, environment) {

  override lazy val ggAuthenticationURL: String = baseUrl("gg-authentication")

  lazy val entityResolverURL: String = baseUrl("entity-resolver")

  lazy val protectedMicroServiceTestOnlyUrl = s"$protectedMicroServiceUrl/income-tax-subscription/test-only"

  lazy val taxEnrolmentsURL: String = baseUrl("tax-enrolments")

  lazy val matchingStubsURL: String = baseUrl("matching-stubs")

  lazy val enrolmentStoreStubUrl: String = baseUrl("enrolment-store-stub")

}

// $COVERAGE-ON$
