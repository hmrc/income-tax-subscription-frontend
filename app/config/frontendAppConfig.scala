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

  def configuration: Configuration
  def appName: String

  def contactFormServiceIdentifier: String
  def ggSignInContinueUrl: String
  def subscriptionUrl: String
  def throttlingUrl: String
  def mandationStatusUrl: String

  def prePopUrl(nino: String): String

  def userMatchingUrl: String
  def clientMatchingUrl: String
  def signUpUrl: String
  def createIncomeSourcesUrl: String
  def authUrl: String
  def preferencesFrontend: String
  def preferencesFrontendRedirect: String
  def baseUrl: String
  def ggUrl: String
  def microServiceUrl: String
  def timeoutWarningInSeconds: String
  def timeoutInSeconds: String
  def channelPreferencesUrl: String

  def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String

  def btaUrl: String
  def wrongCredentials: String
  def haveSaUtr: String
  def btaBaseUrl: String
  def agentServicesUrl: String
  def agentServicesAccountHomeUrl: String
  def agentClientRelationshipsUrl: String
  def authenticatorUrl: String
  def hasEnabledTestOnlyRoutes: Boolean
  def identityVerificationURL: String
  def identityVerificationRequiredConfidenceLevel: ConfidenceLevel
  def govukGuidanceLink: String
  def govukGuidanceITSASignUpIndivLink: String
  def govukGuidanceITSASignUpAgentLink: String
  def govukGuidanceITSAWhoCanSignUpVoluntarily: String
  def citizenDetailsURL: String
  def matchingAttempts: Int
  def matchingLockOutSeconds: Int
  def urBannerUrl: String

  def redirectToLogin(continueUrl: String): Result

  def ggLoginUrl: String

  def getAllocatedEnrolmentUrl(enrolmentKey: EnrolmentKey): String

  def queryUsersUrl(utr: String): String

  def upsertEnrolmentUrl(enrolmentKey: String): String

  def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String

  def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String

  def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String

  def assignEnrolmentUrl(userId: String, enrolmentKey: String): String

  def signUpToSaLink: String
  def agentSignUpUrl: String

  def backendFeatureSwitchUrl: String

  def incomeTaxEligibilityUrl: String

  def incomeTaxSelfEmploymentsFrontendUrl: String

  def incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String

  def incomeTaxSelfEmploymentsFrontendInitialiseUrl: String

  def incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String

  def eligibilityFeatureSwitchUrl: String

  def individualSigningUpUrl: String

  def agentSigningUpUrl: String

  def betaFeedbackUnauthenticatedUrl: String

  def feedbackFrontendRedirectUrl: String

  def feedbackFrontendAgentRedirectUrl: String

}

@Singleton
class FrontendAppConfig @Inject()(config: ServicesConfig, val configuration: Configuration) extends AppConfig {

  lazy val appName: String = config.getString("appName")

  // AutoEnrolment links
  private lazy val usersGroupsSearchUrl: String = config.baseUrl("users-groups-search")

  def getUsersForGroupUrl(groupId: String): String = s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  // Frontend Config
  override lazy val baseUrl: String = config.getString("base.url")
  val contextRoute = "/report-quarterly/income-and-expenses/sign-up"

  //Authentication/Authorisation Config
  override lazy val ggSignInContinueUrl = s"$baseUrl$contextRoute"
  override lazy val authUrl: String = config.baseUrl("auth")

  // sign out
  override lazy val ggUrl: String = config.getString(s"government-gateway.url")

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/bas-gateway/sign-out-without-state?continue=$redirectionUrl"

  def redirectToLogin(continueUrl: String): Result = {
    val basGatewayBaseUrl = config.getString("bas-gateway-frontend.url")
    val basGatewaySignInURI = "/gg/sign-in"
    val origin = config.getString("appName")

    Redirect(
      url = basGatewayBaseUrl + basGatewaySignInURI,
      queryStringParams = Map(
        "continue" -> Seq(continueUrl),
        "origin" -> Seq(origin)
      )
    )
  }

  override lazy val ggLoginUrl: String = s"${config.getString("bas-gateway-frontend.url")}/gg/sign-in"

  // BTA link
  override lazy val btaUrl: String = config.getString(s"bta.url")

  //Contact Frontend Config
  private lazy val contactHost: String = config.getString("contact-frontend.host")
  override lazy val contactFormServiceIdentifier = "MTDIT"

  // protected microservice
  override lazy val microServiceUrl: String = config.baseUrl("income-tax-subscription")
  override lazy val subscriptionUrl = s"$microServiceUrl/income-tax-subscription/subscription"
  override lazy val userMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override lazy val clientMatchingUrl = s"$microServiceUrl/income-tax-subscription/client-matching"
  override lazy val signUpUrl = s"$microServiceUrl/income-tax-subscription/mis/sign-up"
  override lazy val createIncomeSourcesUrl = s"$microServiceUrl/income-tax-subscription/mis/create"
  override lazy val channelPreferencesUrl: String = config.baseUrl("channel-preferences")
  override lazy val throttlingUrl = s"$microServiceUrl/income-tax-subscription/throttled"
  override lazy val mandationStatusUrl = s"$microServiceUrl/income-tax-subscription/itsa-status"

  def prePopUrl(nino: String): String = s"$microServiceUrl/income-tax-subscription/pre-pop/$nino"

  //agent frontend
  private lazy val agentFrontendUrl: String = config.getString("income-tax-subscription-agent-frontend.url")
  override lazy val agentSignUpUrl = s"$agentFrontendUrl/report-quarterly/income-and-expenses/sign-up/client"

  // Digital Preferences
  override lazy val preferencesFrontend: String = config.baseUrl("preferences-frontend")

  override lazy val preferencesFrontendRedirect: String = config.getString("preferences-frontend.url")

  override lazy val btaBaseUrl: String = config.getString("bta.baseUrl")
  override lazy val wrongCredentials: String = s"$btaBaseUrl/business-account/wrong-credentials"
  override lazy val haveSaUtr: String = s"$btaBaseUrl/business-account/add-tax/self-assessment/have-sa-utr"

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


  override lazy val govukGuidanceLink: String = config.getString("govuk-guidance.url")
  override lazy val govukGuidanceITSASignUpIndivLink: String = s"$govukGuidanceLink/sign-up-your-business-for-making-tax-digital-for-income-tax"
  override lazy val govukGuidanceITSASignUpAgentLink: String = s"$govukGuidanceLink/sign-up-your-client-for-making-tax-digital-for-income-tax"
  override lazy val govukGuidanceITSAWhoCanSignUpVoluntarily: String = s"$govukGuidanceLink/sign-up-your-business-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"

  override lazy val citizenDetailsURL: String = config.baseUrl("citizen-details")

  // Agent Services config
  override lazy val agentServicesUrl: String = config.getString("agent-services.url")

  override lazy val agentServicesAccountHomeUrl: String = config.getString("agent-services-frontend.url")

  override lazy val agentClientRelationshipsUrl: String = config.baseUrl("agent-client-relationships")

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

  override lazy val signUpToSaLink: String = config.getString("sa-signup.url")

  override lazy val backendFeatureSwitchUrl: String =
    s"$microServiceUrl/income-tax-subscription/test-only/feature-switch"

  override lazy val incomeTaxEligibilityUrl: String = s"${config.baseUrl("income-tax-subscription-eligibility")}/income-tax-subscription-eligibility"

  override lazy val individualSigningUpUrl: String = s"${controllers.eligibility.individual.routes.SigningUpController.show.url}"

  override lazy val agentSigningUpUrl: String = s"${controllers.eligibility.agent.routes.SigningUpController.show.url}"

  override lazy val incomeTaxSelfEmploymentsFrontendUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments"
  }

  override lazy val incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments/details/business-check-your-answers"
  }

  override lazy val agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = {
    s"${config.getString("income-tax-subscription-self-employed-frontend.url")}/report-quarterly/income-and-expenses/sign-up/self-employments/client/details/business-check-your-answers"
  }

  override lazy val incomeTaxSelfEmploymentsFrontendInitialiseUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/details"
  }

  override lazy val incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String = {
    s"$incomeTaxSelfEmploymentsFrontendUrl/client/details"
  }

  override lazy val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

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

  override lazy val timeoutWarningInSeconds: String = config.getString("session-timeout.warning")
  override lazy val timeoutInSeconds: String = config.getString("session-timeout.seconds")

}
