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
  val betaFeedbackUnauthenticatedUrl: String
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
  val throttleControlUrl: String
  val authUrl: String
  val preferencesService: String
  val preferencesUrl: String
  val baseUrl: String
  val enableThrottling: Boolean
  val ggUrl: String
  val ggSignOutUrl: String
  val btaUrl: String
  val showGuidance: Boolean
  val whitelistIps: Seq[String]
  val ipExclusionList: Seq[Call]
  val shutterPage: String
  val agentServicesUrl: String
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
  override lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  override lazy val contactFormServiceIdentifier = "MTDIT"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  // protected microservice
  protected lazy val protectedMicroServiceUrl = baseUrl("subscription-service")
  override lazy val subscriptionUrl = s"$protectedMicroServiceUrl/income-tax-subscription/subscription"
  override lazy val throttleControlUrl = s"$protectedMicroServiceUrl/income-tax-subscription/throttle"

  // Digital Preferences
  override lazy val preferencesService = baseUrl("preferences-frontend")
  override lazy val preferencesUrl = loadConfig("preferences.url")

  // Enable or disable calling the throttling control in the middle service from the HomeController
  override lazy val enableThrottling = loadConfig("feature-switch.enable-throttling").toBoolean

  // Enable or disable showing the guidance page or go straight to sign ups
  override lazy val showGuidance: Boolean = loadConfig("feature-switch.show-guidance").toBoolean

  // Shutter page
  override lazy val shutterPage: String = loadConfig("shutter-page.url")

  // Whitelisting config
  private def whitelistConfig(key: String): Seq[String] = configuration.getString(key).fold(Seq[String]())(ips => ips.split(",").toSeq)
  override lazy val whitelistIps: Seq[String] = whitelistConfig("ip-whitelist.urls")
  override lazy val ipExclusionList: Seq[Call] = whitelistConfig("ip-whitelist.excludeCalls").map(ip => Call("GET",ip))

  // Agent Services config
  override lazy val agentServicesUrl: String = loadConfig("agent-services.url")

}

