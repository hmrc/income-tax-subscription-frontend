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
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig extends FeatureSwitching {

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
  val showGuidance: Boolean
  val whitelistIps: Seq[String]
  val ipExclusionList: Seq[Call]
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

  def upsertEnrolmentUrl(enrolmentKey: String): String

  def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String

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

  def routeToSwitchLanguage = (lang: String) => controllers.language.routes.LanguageSwitchController.switchToLanguage(lang)

  def languageTranslationEnabled: Boolean

  def betaFeedbackUrl: String

  def betaFeedbackUnauthenticatedUrl: String

  val unplannedOutagePageMainContent: String

  val unplannedOutagePageRelatedContent: String

}

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration,
                                  environment: Environment) extends AppConfig with ServicesConfig {

  override lazy val mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  protected def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  protected def splitString(value: String, separator: String): Seq[String] = value.split(separator).toSeq

  // Frontend Config
  override lazy val baseUrl: String = loadConfig("base.url")
  val contextRoute = "/report-quarterly/income-and-expenses/sign-up"

  //Authentication/Authorisation Config
  override lazy val ggSignInContinueUrl = s"$baseUrl$contextRoute/index"
  override lazy val authUrl = baseUrl("auth")

  // sign out
  override lazy val ggUrl = loadConfig(s"government-gateway.url")

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl) = s"$ggUrl/gg/sign-out?continue=$redirectionUrl"

  // BTA link
  override lazy val btaUrl = loadConfig(s"bta.url")

  // Software choices link
  override lazy val softwareUrl: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  // Agent Auth link
  override lazy val agentAuthUrl = loadConfig(s"agent-auth.url")

  // Agent Services Account link
  override lazy val agentAccountUrl = loadConfig(s"agent-account.url")

  //Contact Frontend Config
  protected lazy val contactFrontendService = baseUrl("contact-frontend")
  protected lazy val contactHost = loadConfig("contact-frontend.host")
  override lazy val contactFormServiceIdentifier = "MTDIT"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  // protected microservice
  protected lazy val protectedMicroServiceUrl = baseUrl("subscription-service")
  override lazy val subscriptionUrl = s"$protectedMicroServiceUrl/income-tax-subscription/subscription"
  override lazy val subscriptionUrlPost = s"$protectedMicroServiceUrl/income-tax-subscription/subscription-v2"
  override lazy val userMatchingUrl = s"$protectedMicroServiceUrl/income-tax-subscription/client-matching"
  override lazy val clientMatchingUrl = s"$protectedMicroServiceUrl/income-tax-subscription/client-matching"

  override def storeNinoUrl(token: String) = s"$protectedMicroServiceUrl/income-tax-subscription/identifier-mapping/$token"

  //agent frontend
  protected lazy val agentFrontendUrl = loadConfig("income-tax-subscription-agent-frontend.url")
  override lazy val agentSignUpUrl = s"$agentFrontendUrl/report-quarterly/income-and-expenses/sign-up/client"

  // Digital Preferences
  override lazy val preferencesFrontend = baseUrl("preferences-frontend")

  override lazy val preferencesFrontendRedirect = loadConfig("preferences-frontend.url")

  override lazy val preferencesUrl = baseUrl("preferences")

  // Enable or disable showing the guidance page or go straight to sign ups
  override lazy val showGuidance: Boolean = loadConfig("feature-switch.show-guidance").toBoolean

  override lazy val shutterPage: String = loadConfig("shutter-page.url")

  private def whitelistConfig(key: String): Seq[String] = configuration.getString(key).fold(Seq[String]())(ips => ips.split(",").toSeq)

  override lazy val whitelistIps: Seq[String] = whitelistConfig("ip-whitelist.urls")

  override lazy val ipExclusionList: Seq[Call] = whitelistConfig("ip-whitelist.excludeCalls").map(ip => Call("GET", ip))

  override lazy val ggAuthenticationURL = baseUrl("gg-authentication")
  override lazy val ggURL = baseUrl("government-gateway")

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

  lazy val taxEnrolments = baseUrl("tax-enrolments")

  override def upsertEnrolmentUrl(enrolmentKey: String): String =
    s"$taxEnrolments/tax-enrolments/enrolments/$enrolmentKey"

  override def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String =
    s"$taxEnrolments/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"

  override def languageTranslationEnabled: Boolean = isEnabled(featureswitch.WelshLanguageFeature)

  override lazy val betaFeedbackUrl: String =
    s"$contactFrontendService/contact/beta-feedback?service=$contactFormServiceIdentifier"

  override lazy val betaFeedbackUnauthenticatedUrl: String =
    s"$contactFrontendService/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  override lazy val unplannedOutagePageMainContent: String = loadConfig("unplanned-outage.main")

  override lazy val unplannedOutagePageRelatedContent: String = loadConfig("unplanned-outage.related")

}

