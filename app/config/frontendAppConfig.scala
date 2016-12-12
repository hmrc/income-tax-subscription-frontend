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
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val notAuthorisedRedirectUrl: String
  val ivRegisterUrl: String
  val ivUpliftUrl: String
  val twoFactorUrl: String
  val ggSignInUrl: String
  val ggSignInContinueUrl: String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  override val ivRegisterUrl = configuration.getString(s"identity-verification.registration.url").getOrElse("")
  override val ivUpliftUrl = configuration.getString(s"identity-verification.uplift.url").getOrElse("")
  override val ggSignInUrl = configuration.getString(s"government-gateway-sign-in.url").getOrElse("")
  override val twoFactorUrl = configuration.getString(s"two-factor.url").getOrElse("")
  override val notAuthorisedRedirectUrl = configuration.getString(s"two-factor.host").getOrElse("")
  override val ggSignInContinueUrl = configuration.getString("government-gateway-sign-in.continue.url").getOrElse("")

}
