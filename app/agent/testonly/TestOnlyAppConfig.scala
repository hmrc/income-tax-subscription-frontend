/*
 * Copyright 2017 HM Revenue & Customs
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

package agent.testonly

import javax.inject.{Inject, Singleton}

import core.config.FrontendAppConfig
import play.api.Application

@Singleton
class TestOnlyAppConfig @Inject()(app: Application) extends FrontendAppConfig(app) {

  lazy val ggStubsURL: String = baseUrl("gg-stubs")

  lazy val preferencesURL: String = baseUrl("preferences")

  lazy val entityResolverURL: String = baseUrl("entity-resolver")

  lazy val protectedMicroServiceTestOnlyUrl = s"$protectedMicroServiceUrl/income-tax-subscription/test-only"

  lazy val matchingStubsURL: String = baseUrl("matching-stubs")

}

// $COVERAGE-ON$
