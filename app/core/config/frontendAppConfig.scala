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

package core.config

import core.config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig extends FeatureSwitching {

  val appName: String

  val contactFormServiceIdentifier: String
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val ggSignInContinueUrl: String
  val subscriptionUrl: String
  val subscriptionUrlPost: String
  val userMatchingUrl: String
  val clientMatchingUrl: String
  val authUrl: String
  val preferencesFrontend: String
  val preferencesFrontendRedirect: String
  val preferencesUrl: String
  val baseUrl: String
  val ggUrl: String

  def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String

  val btaUrl: String
  val softwareUrl: String
  val agentAuthUrl: String
  val agentAccountUrl: String
  val shutterPage: String
  val ggURL: String
  val agentServicesUrl: String
  val agentMicroserviceUrl: String
  val authenticatorUrl: String
  val hasEnabledTestOnlyRoutes: Boolean
  val ggAuthenticationURL: String
  val identityVerificationURL: String
  val contactHmrcLink: String
  val citizenDetailsURL: String
  val matchingAttempts: Int
  val matchingLockOutSeconds: Int

  def enableRegistration: Boolean

  def storeNinoUrl(token: String): String

  def getAllocatedEnrolmentUrl(utr: String): String

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

  val eligibilityFeatureSwitchUrl: String

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => controllers.language.routes.LanguageSwitchController.switchToLanguage(lang)

  def betaFeedbackUrl: String

  def betaFeedbackUnauthenticatedUrl: String

}

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration, environment: Environment) extends AppConfig with ServicesConfig {

  val appName: String = getString("appName")

  // AutoEnrolment links
  def usersGroupsSearchUrl: String = baseUrl("users-groups-search")

  def getUsersForGroupUrl(groupId: String): String = s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  override lazy val mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  protected def loadConfig(key: String): String = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  protected def splitString(value: String, separator: String): Seq[String] = value.split(separator).toSeq

  // Frontend Config
  override lazy val baseUrl: String = loadConfig("base.url")
  val contextRoute = "/report-quarterly/income-and-expenses/sign-up"

  //Authentication/Authorisation Config
  override lazy val ggSignInContinueUrl = s"$baseUrl$contextRoute/index"
  override lazy val authUrl: String = baseUrl("auth")

  // sign out
  override lazy val ggUrl: String = loadConfig(s"government-gateway.url")

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/gg/sign-out?continue=$redirectionUrl"

  // BTA link
  override lazy val btaUrl: String = loadConfig(s"bta.url")

  // Software choices link
  override lazy val softwareUrl: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  // Agent Auth link
  override lazy val agentAuthUrl: String = loadConfig(s"agent-auth.url")

  // Agent Services Account link
  override lazy val agentAccountUrl: String = loadConfig(s"agent-account.url")

  //Contact Frontend Config
  protected lazy val contactFrontendService: String = baseUrl("contact-frontend")
  protected lazy val contactHost: String = loadConfig("contact-frontend.host")
  override lazy val contactFormServiceIdentifier = "MTDIT"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  // protected microservice
  protected lazy val protectedMicroServiceUrl: String = baseUrl("subscription-service")
  override lazy val subscriptionUrl = s"$protectedMicroServiceUrl/income-tax-subscription/subscription"
  override lazy val subscriptionUrlPost = s"$protectedMicroServiceUrl/income-tax-subscription/subscription-v2"
  override lazy val userMatchingUrl = s"$protectedMicroServiceUrl/income-tax-subscription/client-matching"
  override lazy val clientMatchingUrl = s"$protectedMicroServiceUrl/income-tax-subscription/client-matching"

  override def storeNinoUrl(token: String): String = s"$protectedMicroServiceUrl/income-tax-subscription/identifier-mapping/$token"

  //agent frontend
  protected lazy val agentFrontendUrl: String = loadConfig("income-tax-subscription-agent-frontend.url")
  override lazy val agentSignUpUrl = s"$agentFrontendUrl/report-quarterly/income-and-expenses/sign-up/client"

  // Digital Preferences
  override lazy val preferencesFrontend: String = baseUrl("preferences-frontend")

  override lazy val preferencesFrontendRedirect: String = loadConfig("preferences-frontend.url")

  override lazy val preferencesUrl: String = baseUrl("preferences")

  override lazy val shutterPage: String = loadConfig("shutter-page.url")

  override lazy val ggAuthenticationURL: String = baseUrl("gg-authentication")
  override lazy val ggURL: String = baseUrl("government-gateway")

  override lazy val identityVerificationURL: String = loadConfig("identity-verification-frontend.url")

  override lazy val contactHmrcLink: String = loadConfig("contact-hmrc.url")

  override lazy val citizenDetailsURL: String = baseUrl("citizen-details")

  // Agent Services config
  override lazy val agentServicesUrl: String = loadConfig("agent-services.url")

  override lazy val agentMicroserviceUrl: String = baseUrl("agent-microservice")

  /*
  *  This checks to see if the testOnlyDoNotUseInAppConf route is set in configuration instead of the default prod.Routes
  *  This flag can be used by the application to check if the test only routes are enabled. i.e. this flag can be used to
  *  determine the service is not running in the prod environment
  *
  *  One usage of this is in ClientMatchingService where we determine if a "True-Client-IP" should be added for the purpose of
  *  matching.
  */
  override lazy val hasEnabledTestOnlyRoutes: Boolean =
    configuration.getString("application.router").get == "testOnlyDoNotUseInAppConf.Routes"

  override lazy val matchingAttempts: Int = loadConfig("lockout.maxAttempts").toInt

  override lazy val matchingLockOutSeconds: Int = loadConfig("lockout.lockOutSeconds").toInt

  override lazy val authenticatorUrl: String = baseUrl("authenticator")

  override def enableRegistration: Boolean = isEnabled(featureswitch.RegistrationFeature)

  override lazy val addressLookupFrontendURL: String = baseUrl("address-lookup-frontend")

  override lazy val signUpToSaLink: String = loadConfig("sa-signup.url")

  override lazy val sendSAReturnLink: String = loadConfig("sa-return.url")

  override lazy val backendFeatureSwitchUrl: String =
    s"$protectedMicroServiceUrl/income-tax-subscription/test-only/feature-switch"

  override lazy val incomeTaxEligibilityUrl: String = s"${baseUrl("income-tax-subscription-eligibility")}/income-tax-subscription-eligibility"

  override val incomeTaxEligibilityFrontendUrl: String = {
    s"${loadConfig("income-tax-subscription-eligibility-frontend.url")}/report-quarterly/income-and-expenses/sign-up/eligibility"
  }

  override lazy val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

  lazy val taxEnrolments: String = baseUrl("tax-enrolments")

  lazy val enrolmentStoreProxyUrl: String = baseUrl("enrolment-store-proxy") + "/enrolment-store-proxy/enrolment-store"

  def getAllocatedEnrolmentUrl(utr: String): String =
    s"$enrolmentStoreProxyUrl/enrolments/IR-SA~UTR~$utr/groups"

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
    s"$contactFrontendService/contact/beta-feedback?service=$contactFormServiceIdentifier"

  override lazy val betaFeedbackUnauthenticatedUrl: String =
    s"$contactFrontendService/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

}

