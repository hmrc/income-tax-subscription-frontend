/*
 * Copyright 2018 HM Revenue & Customs
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

package core.config

import play.api.mvc.Call

trait MockConfig extends AppConfig {
  override val analyticsToken: String = ""
  override val analyticsHost: String = ""
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val ggSignInContinueUrl: String = "/income-tax-subscription-frontend"
  override val contactFormServiceIdentifier: String = "MTDIT"
  override val contactFrontendPartialBaseUrl: String = "/contact/partial"
  override val subscriptionUrl: String = "/income-tax-subscription/subscription"
  override val clientMatchingUrl = "/income-tax-subscription/client-matching"
  override val agentMicroserviceUrl: String = "/agent-subscription"
  override val preferencesFrontend: String = ""
  override val preferencesFrontendRedirect: String = ""
  override val preferencesUrl: String = ""
  override val baseUrl: String = ""
  override val authUrl: String = ""
  override lazy val authenticatorUrl: String = ""
  override val userMatchingUrl = "/income-tax-subscription/user-matching"
  override lazy val ggUrl = ""

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl) = s"$ggUrl/gg/sign-out?continue=$redirectionUrl"

  override lazy val btaUrl = "https://www.tax.service.gov.uk/business-account"
  override val showGuidance: Boolean = true
  override lazy val shutterPage = "https://www.tax.service.gov.uk/outage-income-tax-subscription/"
  override lazy val whitelistIps: Seq[String] = Seq("127.0.0.1")
  override lazy val ipExclusionList: Seq[Call] = Nil
  override val matchingAttempts: Int = 3
  override val matchingLockOutSeconds: Int = 60

  override val ggAdminURL: String = "/gg-admin"
  override val ggURL: String = "/gg"
  override val ggAuthenticationURL: String = "/gg-auth"

  override val identityVerificationURL: String = ""

  override val contactHmrcLink: String = "https://www.gov.uk/contact-hmrc"

  override val citizenDetailsURL: String = ""

  override val hasEnabledTestOnlyRoutes: Boolean = false

  override val userMatchingFeature: Boolean = false

  override val enableRegistration: Boolean = false

  override val emacEs6ApiEnabled: Boolean = false

  override val emacEs8ApiEnabled: Boolean = false

  override def unauthorisedAgentEnabled: Boolean = false

  override def storeNinoUrl(token: String): String = s"income-tax-subscription/identifier-mapping/$token"

  override lazy val addressLookupFrontendURL: String = ""

  override def newIncomeSourceFlowEnabled: Boolean = isEnabled(featureswitch.NewIncomeSourceFlowFeature)

  override def taxYearDeferralEnabled: Boolean = isEnabled(featureswitch.TaxYearDeferralFeature)

  override val signUpToSaLink: String = "sa-sign-up"

  override val agentSignUpUrl: String = "/report-quarterly/income-and-expenses/sign-up/client"

  override lazy val agentServicesUrl: String = "/agent-subscription"

  override lazy val agentAuthUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"

  override lazy val agentAccountUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"

  override lazy val sendSAReturnLink = "https://www.gov.uk/self-assessment-tax-returns/sending-return"

  override lazy val backendFeatureSwitchUrl = "income-tax-subscription/"

  override def upsertEnrolmentUrl(enrolmentKey: String): String = "enrolment-store/"

  override def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String = "enrolment-store/"

  override def storeSubscriptionUrl(nino: String): String = ""

  override def getAgencyNameUrl(arn: String): String = s"/agent-services-account/client/agency-name/$arn"
}

object MockConfig extends MockConfig