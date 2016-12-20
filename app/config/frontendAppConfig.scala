/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.Play.{configuration, current}
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
  val ivUpliftUrl: String
  val twoFactorUrl: String
  val ggSignInUrl: String
  val ggSignInContinueUrl: String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val baseUrl = "income-tax-subscription-frontend"

  //Authentication/Authorisation Config
  override val ggSignInUrl = loadConfig("government-gateway.sign-in.url")
  override val ggSignInContinueUrl = loadConfig("government-gateway.continue.url")
  override val twoFactorUrl = loadConfig("two-factor.url")
  override val ivUpliftUrl = loadConfig("identity-verification.uplift.url")
  override val notAuthorisedRedirectUrl = loadConfig("not-authorised-callback.url")

  //GA Config
  override val analyticsToken: String = loadConfig(s"google-analytics.token")
  override val analyticsHost: String = loadConfig(s"google-analytics.host")

  //Contact Frontend Config
  private val contactFrontendService = baseUrl("contact-frontend")
  override val betaFeedbackUrl = s"$baseUrl/feedback"
  override val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  override val contactFormServiceIdentifier = "IRS"
  override val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override val reportAProblemPartialUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override val reportAProblemNonJSUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
}
