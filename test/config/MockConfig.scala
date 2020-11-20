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

package config

trait MockConfig extends AppConfig {

  override val appName: String = "app"

  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val ggSignInContinueUrl: String = "/income-tax-subscription-frontend"
  override val contactFormServiceIdentifier: String = "MTDIT"
  override val contactFrontendPartialBaseUrl: String = "/contact/partial"
  override val subscriptionUrl: String = "/income-tax-subscription/subscription"
  override val subscriptionUrlPost: String = "/income-tax-subscription/subscription-v2"
  override val clientMatchingUrl = "/income-tax-subscription/client-matching"
  override val signUpIncomeSourcesUrl = "/income-tax-subscription/mis/sign-up"
  override val createIncomeSourcesUrl = "/income-tax-subscription/mis/create"
  override val microServiceUrl: String = "subscription-service"
  override val agentMicroserviceUrl: String = "/agent-subscription"
  override val preferencesFrontend: String = ""
  override val preferencesFrontendRedirect: String = ""
  override val preferencesUrl: String = ""
  override val baseUrl: String = ""
  override val authUrl: String = ""
  override lazy val authenticatorUrl: String = ""
  override val userMatchingUrl = "/income-tax-subscription/user-matching"
  override lazy val ggUrl = ""

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/gg/sign-out?continue=$redirectionUrl"

  override lazy val btaUrl = "https://www.tax.service.gov.uk/business-account"
  override lazy val softwareUrl = "https://www.gov.uk/guidance/software-for-sending-income-tax-updates"
  override lazy val shutterPage = "https://www.tax.service.gov.uk/outage-income-tax-subscription/"
  override val matchingAttempts: Int = 3
  override val matchingLockOutSeconds: Int = 60

  override val ggURL: String = "/gg"
  override val ggAuthenticationURL: String = "/gg-auth"

  override val identityVerificationURL: String = ""

  override val contactHmrcLink: String = "https://www.gov.uk/contact-hmrc"

  override val citizenDetailsURL: String = ""

  override val hasEnabledTestOnlyRoutes: Boolean = false

  override def storeNinoUrl(token: String): String = s"income-tax-subscription/identifier-mapping/$token"

  override lazy val addressLookupFrontendURL: String = ""

  override val signUpToSaLink: String = "sa-sign-up"

  override val agentSignUpUrl: String = "/report-quarterly/income-and-expenses/sign-up/client"

  override lazy val agentServicesUrl: String = "/agent-subscription"

  override lazy val agentAuthUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"

  override lazy val agentAccountUrl = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"

  override lazy val sendSAReturnLink = "https://www.gov.uk/self-assessment-tax-returns/sending-return"

  override lazy val backendFeatureSwitchUrl = "income-tax-subscription/"

  override def upsertEnrolmentUrl(enrolmentKey: String): String = "tax-enrolments/"

  override def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String = "tax-enrolments/"

  override val betaFeedbackUrl: String = "/feedback"

  override val betaFeedbackUnauthenticatedUrl: String = "/feedback"

  override lazy val incomeTaxEligibilityUrl: String = "/income-tax-subscription-incometax.eligibility"

  override val incomeTaxEligibilityFrontendUrl: String = "/report-quarterly/income-and-expenses/sign-up/eligibility"

  override lazy val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

  override val incomeTaxSelfEmploymentsFrontendUrl: String = "/report-quarterly/income-and-expenses/sign-up/self-employments"
  override val incomeTaxSelfEmploymentsFrontendInitialiseUrl: String = s"$incomeTaxSelfEmploymentsFrontendUrl/details"
  override val incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String = s"$incomeTaxSelfEmploymentsFrontendUrl/client/details"

  override def getAllocatedEnrolmentUrl(utr: String): String = ???

  override def queryUsersUrl(utr: String): String = ???

  override def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String = ???

  override def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String = ???

  override def assignEnrolmentUrl(userId: String, enrolmentKey: String): String = ???

}

object MockConfig extends MockConfig
