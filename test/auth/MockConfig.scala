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

package auth

import config.AppConfig
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
  override val contactFormServiceIdentifier: String = "MTDIT"
  override val contactFrontendPartialBaseUrl: String = "/contact/partial"
  override val alreadyEnrolledUrl: String = "/already-enrolled"
  override val subscriptionUrl: String = "/income-tax-subscription/subscription"
  override val preferencesFrontend: String = ""
  override val preferencesFrontendRedirect: String = ""
  override val preferencesUrl: String = ""
  override val baseUrl: String = ""
  override val authUrl: String = ""
  override lazy val authenticatorUrl: String = ""
  override val userMatchingUrl = "/income-tax-subscription/user-matching"
  override lazy val ggUrl = ""
  override lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$ggSignInContinueUrl"
  override lazy val btaUrl = "https://www.tax.service.gov.uk/business-account"
  override val showGuidance: Boolean = true
  override lazy val shutterPage = "https://www.tax.service.gov.uk/outage-income-tax-subscription/"
  override lazy val whitelistIps: Seq[String] = Seq("127.0.0.1")
  override lazy val ipExclusionList: Seq[Call] = Nil
  override val matchingAttempts: Int = 3
  override val matchingLockOutSeconds: Int = 60

  override val authURL: String = "/auth"
  override val ggAdminURL: String = "/gg-admin"
  override val ggURL: String = "/gg"
  override val ggAuthenticationURL: String = "/gg-auth"

  override val identityVerificationURL: String = ""

  override val contactHmrcLink: String = "https://www.gov.uk/contact-hmrc"

  override val citizenDetailsURL: String = ""

  override val hasEnabledTestOnlyRoutes: Boolean = false

  override val userMatchingFeature: Boolean = false

  override val enableRegistration: Boolean = false

  override def storeNinoUrl(token: String): String = s"income-tax-subscription/identifier-mapping/$token"

  override lazy val addressLookupFrontendURL: String = ""

  override val newPreferencesApiEnabled: Boolean = false
}

object MockConfig extends MockConfig