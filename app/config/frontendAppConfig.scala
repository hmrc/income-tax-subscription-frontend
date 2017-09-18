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

package config

import javax.inject.{Inject, Singleton}

import play.api.mvc.Call
import play.api.{Application, Configuration}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val betaFeedbackUrl: String
  val analyticsToken: String
  val analyticsHost: String
  val contactFormServiceIdentifier: String
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val notAuthorisedRedirectUrl: String
  val ggSignInUrl: String
  val ggSignInContinueUrl: String
  val alreadyEnrolledUrl: String
  val subscriptionUrl: String
  val userMatchingUrl: String
  val authUrl: String
  val preferencesService: String
  val preferencesUrl: String
  val baseUrl: String
  val ggUrl: String
  val ggSignOutUrl: String
  val btaUrl: String
  val showGuidance: Boolean
  val whitelistIps: Seq[String]
  val ipExclusionList: Seq[Call]
  val shutterPage: String
  val authURL: String
  val ggURL: String
  val ggAdminURL: String
  val ggAuthenticationURL: String
  val identityVerificationURL: String
  val contactHmrcLink: String
  val citizenDetailsURL: String
  val hasEnabledTestOnlyRoutes: Boolean
  val matchingAttempts: Int
  val matchingLockOutSeconds: Int
  val authenticatorUrl: String
  val userMatchingFeature: Boolean
  val enableRegistration: Boolean
  def storeNinoUrl(token: String): String
  val newPreferencesAPIFeature: Boolean
}

@Singleton
class FrontendAppConfig @Inject()(val app: Application) extends AppConfig with ServicesConfig {

  protected val configuration: Configuration = app.configuration

  protected def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  protected def splitString(value: String, separator: String): Seq[String] = value.split(separator).toSeq

  // Frontend Config
  override lazy val baseUrl: String = loadConfig("base.url")
  protected val contextRoute = "income-tax-subscription-frontend"

  //Authentication/Authorisation Config
  override lazy val ggSignInUrl = loadConfig("government-gateway.sign-in.url")
  override lazy val ggSignInContinueUrl = loadConfig("government-gateway.continue.url")
  override lazy val notAuthorisedRedirectUrl = loadConfig("not-authorised-callback.url")
  override lazy val alreadyEnrolledUrl = loadConfig("already-enrolled.url")
  override lazy val authUrl = baseUrl("auth")

  // sign out
  override lazy val ggUrl = loadConfig(s"government-gateway.url")
  override lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$ggSignInContinueUrl"

  // BTA link
  override lazy val btaUrl = loadConfig(s"bta.url")

  //GA Config
  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")

  //Contact Frontend Config
  protected lazy val contactFrontendService = baseUrl("contact-frontend")
  protected lazy val contactHost = loadConfig("contact-frontend.host")
  override lazy val betaFeedbackUrl = s"$contextRoute/feedback"
  override lazy val contactFormServiceIdentifier = "MTDIT"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  // protected microservice
  protected lazy val protectedMicroServiceUrl = baseUrl("subscription-service")
  override lazy val subscriptionUrl = s"$protectedMicroServiceUrl/income-tax-subscription/subscription"
  override lazy val userMatchingUrl = s"$protectedMicroServiceUrl/income-tax-subscription/client-matching"
  override def storeNinoUrl(token: String) = s"$protectedMicroServiceUrl/income-tax-subscription/identifier-mapping/$token"

  // Digital Preferences
  override lazy val preferencesService = baseUrl("preferences-frontend")
  override lazy val preferencesUrl = loadConfig("preferences.url")

  // Enable or disable showing the guidance page or go straight to sign ups
  override lazy val showGuidance: Boolean = loadConfig("feature-switch.show-guidance").toBoolean

  override lazy val shutterPage: String = loadConfig("shutter-page.url")

  private def whitelistConfig(key: String): Seq[String] = configuration.getString(key).fold(Seq[String]())(ips => ips.split(",").toSeq)

  override lazy val whitelistIps: Seq[String] = whitelistConfig("ip-whitelist.urls")

  override lazy val ipExclusionList: Seq[Call] = whitelistConfig("ip-whitelist.excludeCalls").map(ip => Call("GET", ip))

  override lazy val authURL = baseUrl("auth")
  override lazy val ggAuthenticationURL = baseUrl("gg-authentication")
  override lazy val ggURL = baseUrl("government-gateway")
  override lazy val ggAdminURL = baseUrl("gg-admin")

  override lazy val identityVerificationURL: String = loadConfig("identity-verification-frontend.url")

  override lazy val contactHmrcLink: String = loadConfig("contact-hmrc.url")

  override lazy val citizenDetailsURL: String = loadConfig("citizen-details.url")


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

  override lazy val userMatchingFeature: Boolean = loadConfig("feature-switch.user-matching").toBoolean

  override lazy val enableRegistration: Boolean = loadConfig("feature-switch.enable-registration").toBoolean

  override val newPreferencesAPIFeature: Boolean = loadConfig("feature-switch.new-preferences-api").toBoolean
}

