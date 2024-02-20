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

package config

import models.common.subscription.EnrolmentKey
import play.api.Configuration
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

trait AppConfig {

  val configuration: Configuration
  val appName: String

  val contactFormServiceIdentifier: String
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val ggSignInContinueUrl: String
  val subscriptionUrl: String
  val subscriptionUrlPost: String
  val throttlingUrl: String
  val mandationStatusUrl: String
  val userMatchingUrl: String
  val clientMatchingUrl: String
  val signUpIncomeSourcesUrl: String
  val createIncomeSourcesUrl: String
  val authUrl: String
  val preferencesFrontend: String
  val preferencesFrontendRedirect: String
  val preferencesUrl: String
  val baseUrl: String
  val ggUrl: String
  val microServiceUrl: String
  val timeoutWarningInSeconds: String
  val timeoutInSeconds: String
  val channelPreferencesUrl: String

  def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String

  val btaUrl: String
  val wrongCredentials: String
  val haveSaUtr: String
  val btaBaseUrl: String
  val softwareUrl: String
  val agentAuthUrl: String
  val agentAccountUrl: String
  val shutterPage: String
  val onlineServiceAccountUrl: String
  val ggURL: String
  val agentServicesUrl: String
  val agentServicesAccountHomeUrl: String
  val agentMicroserviceUrl: String
  val authenticatorUrl: String
  val hasEnabledTestOnlyRoutes: Boolean
  val ggAuthenticationURL: String
  val identityVerificationURL: String
  val identityVerificationRequiredConfidenceLevel: ConfidenceLevel
  val contactHmrcLink: String
  val govukGuidanceLink: String
  val govukGuidanceITSASignUpIndivLink: String
  val govukGuidanceITSASignUpAgentLink: String
  val govukGuidanceITSAQualifyingIncomeLink: String
  val govukGuidanceITSAUsingMTDLink: String
  val govUkSendingReturnsUrl: String
  val citizenDetailsURL: String
  val matchingAttempts: Int
  val matchingLockOutSeconds: Int
  val urBannerUrl: String

  def storeNinoUrl(token: String): String

  def getAllocatedEnrolmentUrl(enrolmentKey: EnrolmentKey): String

  def queryUsersUrl(utr: String): String

  def upsertEnrolmentUrl(enrolmentKey: String): String

  def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String

  def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String

  def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String

  def assignEnrolmentUrl(userId: String, enrolmentKey: String): String

  val addressLookupFrontendURL: String
  val signUpToSaLink: String
  val sendSAReturnLink: String
  val agentSignUpUrl: String


  val backendFeatureSwitchUrl: String

  val incomeTaxEligibilityUrl: String

  def incomeTaxEligibilityFrontendUrl: String

  def incomeTaxSelfEmploymentsFrontendUrl: String

  def incomeTaxSelfEmploymentsFrontendCheckYourAnswersUrl: String

  def incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def incomeTaxSelfEmploymentsFrontendInitialiseUrl: String

  def incomeTaxSelfEmploymentsFrontendBusinessAccountingMethodUrl: String

  def incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String

  def agentIncomeTaxEligibilityFrontendTermsUrl: String

  val incomeTaxViewChangeUrl: String

  val eligibilityFeatureSwitchUrl: String


  def betaFeedbackUrl: String

  def betaFeedbackUnauthenticatedUrl: String

  def feedbackFrontendRedirectUrl: String

  def feedbackFrontendAgentRedirectUrl: String

  def maxSelfEmployments: Int

}

@Singleton
class FrontendAppConfig @Inject()(config: ServicesConfig, val configuration: Configuration) extends AppConfig {

  val appName: String = config.getString("appName")

  // AutoEnrolment links
  def usersGroupsSearchUrl: String = config.baseUrl("users-groups-search")

  def getUsersForGroupUrl(groupId: String): String = s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  protected def splitString(value: String, separator: String): Seq[String] = value.split(separator).toSeq

  // Frontend Config
  lazy val baseUrl: String = config.getString("base.url")
  val contextRoute = "/report-quarterly/income-and-expenses/sign-up"

  //Authentication/Authorisation Config
  override lazy val ggSignInContinueUrl = s"$baseUrl$contextRoute/index"
  override lazy val authUrl: String = config.baseUrl("auth")

  // sign out
  override lazy val ggUrl: String = config.getString(s"government-gateway.url")

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/bas-gateway/sign-out-without-state?continue=$redirectionUrl"

  // BTA link
  override lazy val btaUrl: String = config.getString(s"bta.url")

  // Software choices link
  override lazy val softwareUrl: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  // Agent Auth link
  override lazy val agentAuthUrl: String = config.getString(s"agent-auth.url")

  // Agent Services Account link
  override lazy val agentAccountUrl: String = config.getString(s"agent-account.url")

  //Contact Frontend Config
  protected lazy val contactFrontendService: String = config.baseUrl("contact-frontend")
  protected lazy val contactHost: String = config.getString("contact-frontend.host")
  override lazy val contactFormServiceIdentifier = "MTDIT"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  // protected microservice
  override lazy val microServiceUrl: String = config.baseUrl("subscription-service")
  override lazy val subscriptionUrl = s"$microServiceUrl/income-tax-subscription/subscription"
  override lazy val subscriptionUrlPost = s"$microServiceUrl/income-tax-subscription/subscription-v2"
  override lazy val userMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override lazy val clientMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override lazy val signUpIncomeSourcesUrl = s"$microServiceUrl/income-tax-subscription/mis/sign-up"
  override lazy val createIncomeSourcesUrl = s"$microServiceUrl/income-tax-subscription/mis/create"
  override lazy val channelPreferencesUrl: String = config.baseUrl("channel-preferences")
  override lazy val throttlingUrl = s"$microServiceUrl/income-tax-subscription/throttled"
  override lazy val mandationStatusUrl = s"$microServiceUrl/income-tax-subscription/itsa-status"


  override def storeNinoUrl(token: String): String = s"$microServiceUrl/income-tax-subscription/identifier-mapping/$token"

  //agent frontend
  protected lazy val agentFrontendUrl: String = config.getString("income-tax-subscription-agent-frontend.url")
  override lazy val agentSignUpUrl = s"$agentFrontendUrl/report-quarterly/income-and-expenses/sign-up/client"

  // Digital Preferences
  override lazy val preferencesFrontend: String = config.baseUrl("preferences-frontend")

  override lazy val preferencesFrontendRedirect: String = config.getString("preferences-frontend.url")

  override lazy val preferencesUrl: String = config.baseUrl("preferences")
  override lazy val btaBaseUrl: String = config.getString("bta.baseUrl")
  override lazy val shutterPage: String = config.getString("shutter-page.url")
  override lazy val onlineServiceAccountUrl: String = config.getString("online-services-account.url")
  override val wrongCredentials: String = s"$btaBaseUrl/business-account/wrong-credentials"
  override val haveSaUtr: String = s"$btaBaseUrl/business-account/add-tax/self-assessment/have-sa-utr"
  override lazy val ggAuthenticationURL: String = config.baseUrl("gg-authentication")
  override lazy val ggURL: String = config.baseUrl("government-gateway")

  override lazy val identityVerificationRequiredConfidenceLevel: ConfidenceLevel = {
    ConfidenceLevel.fromInt(config.getInt("identity-verification-frontend.target-confidence-level")).getOrElse(
      throw new InternalServerException(
        s"[FrontendAppConfig][identityVerificationRequiredConfidenceLevel] - configured confidence level not a real confidence level"
      )
    )
  }
  override lazy val identityVerificationURL: String = {
    val identityVerificationFrontendBaseUrl: String = config.getString("identity-verification-frontend.url")
    val upliftUri: String = config.getString("identity-verification-frontend.uplift-uri")
    val origin: String = config.getString("identity-verification-frontend.origin")
    val confidenceLevel: Int = identityVerificationRequiredConfidenceLevel.level
    val successUrl: String = baseUrl + controllers.individual.iv.routes.IVSuccessController.success.url
    val failureUrl: String = baseUrl + controllers.individual.iv.routes.IVFailureController.failure.url

    s"$identityVerificationFrontendBaseUrl$upliftUri?origin=$origin&confidenceLevel=$confidenceLevel&completionURL=$successUrl&failureURL=$failureUrl"
  }


  override lazy val contactHmrcLink: String = config.getString("contact-hmrc.url")
  override lazy val govukGuidanceLink: String = config.getString("govuk-guidance.url")
  override lazy val govukGuidanceITSASignUpIndivLink: String = s"$govukGuidanceLink/sign-up-your-business-for-making-tax-digital-for-income-tax"
  override lazy val govukGuidanceITSASignUpAgentLink: String = s"$govukGuidanceLink/sign-up-your-client-for-making-tax-digital-for-income-tax"
  override lazy val govukGuidanceITSAQualifyingIncomeLink: String = s"$govukGuidanceLink/check-if-youre-eligible-for-making-tax-digital-for-income-tax#find-out-about-qualifying-income"
  override lazy val govukGuidanceITSAUsingMTDLink: String = s"$govukGuidanceLink/using-making-tax-digital-for-income-tax#who-can-use-making-tax-digital-for-income-tax"

  override lazy val govUkSendingReturnsUrl: String = s"https://www.gov.uk/self-assessment-tax-returns/sending-return"

  override lazy val citizenDetailsURL: String = config.baseUrl("citizen-details")

  // Agent Services config
  override lazy val agentServicesUrl: String = config.getString("agent-services.url")

  override lazy val agentServicesAccountHomeUrl: String = config.getString("agent-services-frontend.url")

  override lazy val agentMicroserviceUrl: String = config.baseUrl("agent-microservice")

  /*
  *  This checks to see if the testOnlyDoNotUseInAppConf route is set in configuration instead of the default prod.Routes
  *  This flag can be used by the application to check if the test only routes are enabled. i.e. this flag can be used to
  *  determine the service is not running in the prod environment
  *
  *  One usage of this is in ClientMatchingService where we determine if a "True-Client-IP" should be added for the purpose of
  *  matching.
  */
  override lazy val hasEnabledTestOnlyRoutes: Boolean =
    config.getString("play.http.router") == "testOnlyDoNotUseInAppConf.Routes"

  override lazy val matchingAttempts: Int = config.getString("lockout.maxAttempts").toInt

  override lazy val matchingLockOutSeconds: Int = config.getString("lockout.lockOutSeconds").toInt

  override lazy val authenticatorUrl: String = config.baseUrl("authenticator")

  override lazy val addressLookupFrontendURL: String = config.baseUrl("address-lookup-frontend")

  override lazy val signUpToSaLink: String = config.getString("sa-signup.url")

  override lazy val sendSAReturnLink: String = config.getString("sa-return.url")

  override lazy val backendFeatureSwitchUrl: String =
    s"$microServiceUrl/income-tax-subscription/test-only/feature-switch"

  override lazy val incomeTaxEligibilityUrl: String = s"${config.baseUrl("income-tax-subscription-eligibility")}/income-tax-subscription-eligibility"

  override val incomeTaxEligibilityFrontendUrl: String = {
    s"${config.getString("income-tax-subscription-eligibility-frontend.url")}/report-quarterly/income-and-expenses/sign-up/eligibility"
  }

  override val agentIncomeTaxEligibilityFrontendTermsUrl: String = {
    s"${config.getString("income-tax-subscription-eligibility-frontend.url")}/report-quarterly/income-and-expenses/sign-up/eligibility/client/what-you-need-to-do"
  }

  override val incomeTaxSelfEmploymentsFrontendUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments"
  }

  override val incomeTaxSelfEmploymentsFrontendCheckYourAnswersUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments/details/business-list"
  }

  override val incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments/details/business-check-your-answers"
  }

  override val agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments/client/details/business-check-your-answers"
  }

  override val incomeTaxSelfEmploymentsFrontendInitialiseUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/details"
  }

  override val incomeTaxSelfEmploymentsFrontendBusinessAccountingMethodUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/details/business-accounting-method"
  }

  override val incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/client/details"
  }

  override lazy val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

  lazy val taxEnrolments: String = config.baseUrl("tax-enrolments")

  lazy val enrolmentStoreProxyUrl: String = config.baseUrl("enrolment-store-proxy") + "/enrolment-store-proxy/enrolment-store"

  def getAllocatedEnrolmentUrl(enrolmentKey: EnrolmentKey): String =
    s"$enrolmentStoreProxyUrl/enrolments/${enrolmentKey.asString}/groups"

  def queryUsersUrl(utr: String): String =
    s"$enrolmentStoreProxyUrl/enrolments/IR-SA~UTR~$utr/users"

  def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String = s"$enrolmentStoreProxyUrl/enrolments/$enrolmentKey"

  def assignEnrolmentUrl(userId: String, enrolmentKey: String): String = s"$enrolmentStoreProxyUrl/users/$userId/enrolments/$enrolmentKey"

  def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String = s"$enrolmentStoreProxyUrl/groups/$groupId/enrolments/$enrolmentKey"

  override def upsertEnrolmentUrl(enrolmentKey: String): String =
    s"$taxEnrolments/tax-enrolments/enrolments/$enrolmentKey"

  override def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String =
    s"$taxEnrolments/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"

  override lazy val betaFeedbackUrl: String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"

  override lazy val betaFeedbackUnauthenticatedUrl: String =
    s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  val feedbackFrontendRedirectUrl: String = config.getString("feedback-frontend.url")

  val feedbackFrontendAgentRedirectUrl: String = config.getString("feedback-frontend-A.url")

  val urBannerUrl: String = config.getString("urBannerUrl.url")

  override lazy val timeoutWarningInSeconds: String = config.getString("session-timeout.warning")
  override lazy val timeoutInSeconds: String = config.getString("session-timeout.seconds")

  override val incomeTaxViewChangeUrl: String = {
    s"${config.getString("income-tax-view-change-frontend.url")}/report-quarterly/income-and-expenses/view"
  }

  override val maxSelfEmployments: Int = config.getInt("self-employments.max-amount")

}
