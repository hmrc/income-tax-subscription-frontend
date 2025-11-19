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
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

trait AppConfig {

  val configuration: Configuration
  val appName: String

  val contactFormServiceIdentifier: String
  val ggSignInContinueUrl: String
  val subscriptionUrl: String
  val throttlingUrl: String
  val mandationStatusUrl: String

  def prePopUrl(nino: String): String

  val userMatchingUrl: String
  val clientMatchingUrl: String
  val signUpUrl: String
  val createIncomeSourcesUrl: String
  val authUrl: String
  val preferencesFrontend: String
  val preferencesFrontendRedirect: String
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
  val agentServicesUrl: String
  val agentServicesAccountHomeUrl: String
  val agentClientRelationshipsUrl: String
  val authenticatorUrl: String
  val hasEnabledTestOnlyRoutes: Boolean
  val identityVerificationURL: String
  val identityVerificationRequiredConfidenceLevel: ConfidenceLevel
  val govukGuidanceLink: String
  val govukGuidanceITSASignUpIndivLink: String
  val govukGuidanceITSASignUpAgentLink: String
  val govukGuidanceITSAWhoCanSignUpVoluntarily: String
  val citizenDetailsURL: String
  val matchingAttempts: Int
  val matchingLockOutSeconds: Int
  val urBannerUrl: String

  def redirectToLogin(continueUrl: String): Result

  def ggLoginUrl: String

  def getAllocatedEnrolmentUrl(enrolmentKey: EnrolmentKey): String

  def queryUsersUrl(utr: String): String

  def upsertEnrolmentUrl(enrolmentKey: String): String

  def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String

  def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String

  def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String

  def assignEnrolmentUrl(userId: String, enrolmentKey: String): String

  val signUpToSaLink: String
  val agentSignUpUrl: String

  val backendFeatureSwitchUrl: String

  val incomeTaxEligibilityUrl: String

  def incomeTaxSelfEmploymentsFrontendUrl: String

  def incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def incomeTaxSelfEmploymentsFrontendInitialiseUrl: String

  def incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String

  val eligibilityFeatureSwitchUrl: String

  def individualSigningUpUrl: String

  def agentSigningUpUrl: String

  def betaFeedbackUnauthenticatedUrl: String

  def feedbackFrontendRedirectUrl: String

  def feedbackFrontendAgentRedirectUrl: String

}

@Singleton
class FrontendAppConfig @Inject()(config: ServicesConfig, val configuration: Configuration) extends AppConfig {

  val appName: String = config.getString("appName")

  // AutoEnrolment links
  private lazy val usersGroupsSearchUrl: String = config.baseUrl("users-groups-search")

  def getUsersForGroupUrl(groupId: String): String = s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  // Frontend Config
  override val baseUrl: String = config.getString("base.url")
  val contextRoute = "/report-quarterly/income-and-expenses/sign-up"

  //Authentication/Authorisation Config
  override val ggSignInContinueUrl = s"$baseUrl$contextRoute"
  override val authUrl: String = config.baseUrl("auth")

  // sign out
  override val ggUrl: String = config.getString(s"government-gateway.url")

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/bas-gateway/sign-out-without-state?continue=$redirectionUrl"

  def redirectToLogin(continueUrl: String): Result = {
    val basGatewayBaseUrl = config.getString("bas-gateway-frontend.url")
    val basGatewaySignInURI = "/bas-gateway/sign-in"
    val origin = config.getString("appName")

    Redirect(
      url = basGatewayBaseUrl + basGatewaySignInURI,
      queryStringParams = Map(
        "continue_url" -> Seq(continueUrl),
        "origin" -> Seq(origin)
      )
    )
  }

  override lazy val ggLoginUrl: String = s"${config.getString("bas-gateway-frontend.url")}/bas-gateway/sign-in"

  // BTA link
  override val btaUrl: String = config.getString(s"bta.url")

  //Contact Frontend Config
  private lazy val contactHost: String = config.getString("contact-frontend.host")
  override val contactFormServiceIdentifier = "MTDIT"

  // protected microservice
  override val microServiceUrl: String = config.baseUrl("income-tax-subscription")
  override val subscriptionUrl = s"$microServiceUrl/income-tax-subscription/subscription"
  override val userMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override val clientMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override val signUpUrl = s"$microServiceUrl/income-tax-subscription/mis/sign-up"
  override val createIncomeSourcesUrl = s"$microServiceUrl/income-tax-subscription/mis/create"
  override val channelPreferencesUrl: String = config.baseUrl("channel-preferences")
  override val throttlingUrl = s"$microServiceUrl/income-tax-subscription/throttled"
  override val mandationStatusUrl = s"$microServiceUrl/income-tax-subscription/itsa-status"

  override def prePopUrl(nino: String): String = s"$microServiceUrl/income-tax-subscription/pre-pop/$nino"

  //agent frontend
  private lazy val agentFrontendUrl: String = config.getString("income-tax-subscription-agent-frontend.url")
  override val agentSignUpUrl = s"$agentFrontendUrl/report-quarterly/income-and-expenses/sign-up/client"

  // Digital Preferences
  override val preferencesFrontend: String = config.baseUrl("preferences-frontend")

  override val preferencesFrontendRedirect: String = config.getString("preferences-frontend.url")

  override val btaBaseUrl: String = config.getString("bta.baseUrl")
  override val wrongCredentials: String = s"$btaBaseUrl/business-account/wrong-credentials"
  override val haveSaUtr: String = s"$btaBaseUrl/business-account/add-tax/self-assessment/have-sa-utr"

  override val identityVerificationRequiredConfidenceLevel: ConfidenceLevel = {
    ConfidenceLevel.fromInt(config.getInt("identity-verification-frontend.target-confidence-level")).getOrElse(
      throw new InternalServerException(
        s"[FrontendAppConfig][identityVerificationRequiredConfidenceLevel] - configured confidence level not a real confidence level"
      )
    )
  }
  override val identityVerificationURL: String = {
    val identityVerificationFrontendBaseUrl: String = config.getString("identity-verification-frontend.url")
    val upliftUri: String = config.getString("identity-verification-frontend.uplift-uri")
    val origin: String = config.getString("identity-verification-frontend.origin")
    val confidenceLevel: Int = identityVerificationRequiredConfidenceLevel.level
    val successUrl: String = baseUrl + controllers.individual.iv.routes.IVSuccessController.success.url
    val failureUrl: String = baseUrl + controllers.individual.iv.routes.IVFailureController.failure.url

    s"$identityVerificationFrontendBaseUrl$upliftUri?origin=$origin&confidenceLevel=$confidenceLevel&completionURL=$successUrl&failureURL=$failureUrl"
  }


  override val govukGuidanceLink: String = config.getString("govuk-guidance.url")
  override val govukGuidanceITSASignUpIndivLink: String = s"$govukGuidanceLink/sign-up-your-business-for-making-tax-digital-for-income-tax"
  override val govukGuidanceITSASignUpAgentLink: String = s"$govukGuidanceLink/sign-up-your-client-for-making-tax-digital-for-income-tax"
  override val govukGuidanceITSAWhoCanSignUpVoluntarily: String = s"$govukGuidanceLink/sign-up-your-business-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"

  override val citizenDetailsURL: String = config.baseUrl("citizen-details")

  // Agent Services config
  override val agentServicesUrl: String = config.getString("agent-services.url")

  override val agentServicesAccountHomeUrl: String = config.getString("agent-services-frontend.url")

  override val agentClientRelationshipsUrl: String = config.baseUrl("agent-client-relationships")

  /*
  *  This checks to see if the testOnlyDoNotUseInAppConf route is set in configuration instead of the default prod.Routes
  *  This flag can be used by the application to check if the test only routes are enabled. i.e. this flag can be used to
  *  determine the service is not running in the prod environment
  *
  *  One usage of this is in ClientMatchingService where we determine if a "True-Client-IP" should be added for the purpose of
  *  matching.
  */
  override val hasEnabledTestOnlyRoutes: Boolean =
    config.getString("play.http.router") == "testOnlyDoNotUseInAppConf.Routes"

  override val matchingAttempts: Int = config.getString("lockout.maxAttempts").toInt

  override val matchingLockOutSeconds: Int = config.getString("lockout.lockOutSeconds").toInt

  override val authenticatorUrl: String = config.baseUrl("authenticator")

  override val signUpToSaLink: String = config.getString("sa-signup.url")

  override val backendFeatureSwitchUrl: String =
    s"$microServiceUrl/income-tax-subscription/test-only/feature-switch"

  override val incomeTaxEligibilityUrl: String = s"${config.baseUrl("income-tax-subscription-eligibility")}/income-tax-subscription-eligibility"

  override lazy val individualSigningUpUrl: String = s"${controllers.eligibility.individual.routes.SigningUpController.show.url}"

  override lazy val agentSigningUpUrl: String = s"${controllers.eligibility.agent.routes.SigningUpController.show.url}"

  override val incomeTaxSelfEmploymentsFrontendUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments"
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

  override val incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/client/details"
  }

  override val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

  private lazy val taxEnrolments: String = config.baseUrl("tax-enrolments")

  private lazy val enrolmentStoreProxyUrl: String = config.baseUrl("enrolment-store-proxy") + "/enrolment-store-proxy/enrolment-store"

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

  override lazy val betaFeedbackUnauthenticatedUrl: String =
    s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  val feedbackFrontendRedirectUrl: String = config.getString("feedback-frontend.url")

  val feedbackFrontendAgentRedirectUrl: String = config.getString("feedback-frontend-A.url")

  val urBannerUrl: String = config.getString("urBannerUrl.url")

  override val timeoutWarningInSeconds: String = config.getString("session-timeout.warning")
  override val timeoutInSeconds: String = config.getString("session-timeout.seconds")

}
