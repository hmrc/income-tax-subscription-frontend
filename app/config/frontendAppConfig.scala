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

import play.api.Configuration
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
  val alreadyEnrolledUrl : String
}

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val baseUrl = "income-tax-subscription-frontend"

  //Authentication/Authorisation Config
  override lazy val ggSignInUrl = loadConfig("government-gateway.sign-in.url")
  override lazy val ggSignInContinueUrl = loadConfig("government-gateway.continue.url")
  override lazy val twoFactorUrl = loadConfig("two-factor.url")
  override lazy val ivUpliftUrl = loadConfig("identity-verification.uplift.url")
  override lazy val notAuthorisedRedirectUrl = loadConfig("not-authorised-callback.url")
  override lazy val alreadyEnrolledUrl = loadConfig("already-enrolled.url")

  //GA Config
  override lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost: String = loadConfig(s"google-analytics.host")

  //Contact Frontend Config
  private lazy val contactFrontendService = baseUrl("contact-frontend")
  override lazy val betaFeedbackUrl = s"$baseUrl/feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  override lazy val contactFormServiceIdentifier = "IRS"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
}

