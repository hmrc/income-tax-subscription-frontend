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

package agent.auth

import agent.config.AppConfig
import play.api.mvc.Call

trait MockConfig extends AppConfig {
  override val analyticsToken: String = ""
  override val analyticsHost: String = ""
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val notAuthorisedRedirectUrl: String = "/not-authorised"
  override val ggSignInUrl: String = "/gg/sign-in"
  override val ggSignInContinueUrl: String = "/income-tax-subscription-frontend"
  override val betaFeedbackUrl: String = "/feedback"
  override val contactFormServiceIdentifier: String = "/contact"
  override val contactFrontendPartialBaseUrl: String = "/contact/partial"
  override val subscriptionUrl: String = "/income-tax-subscription/subscription"
  override val clientMatchingUrl = "/income-tax-subscription/client-matching"
  override val agentMicroserviceUrl: String = "/agent-subscription"
  override val baseUrl: String = ""
  override val authUrl: String = ""
  override lazy val ggUrl = ""
  override lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$ggSignInContinueUrl"
  override lazy val agentAuthUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
  override lazy val agentAccountUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
  override val showGuidance: Boolean = true
  override lazy val shutterPage = "https://www.tax.service.gov.uk/outage-income-tax-subscription/"
  override lazy val whitelistIps: Seq[String] = Seq("127.0.0.1")
  override lazy val ipExclusionList: Seq[Call] = Nil
  override lazy val agentServicesUrl: String = "/agent-subscription"
  override lazy val authenticatorUrl = ""
  override lazy val hasEnabledTestOnlyRoutes = false
  override val ggAdminURL: String = ""
  override val matchingAttempts: Int = 3
  override val matchingLockOutSeconds: Int = 60
}

object MockConfig extends MockConfig