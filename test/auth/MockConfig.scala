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

package auth

import config.AppConfig

object MockConfig extends AppConfig {
  override val analyticsToken: String = ""
  override val analyticsHost: String = ""
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val notAuthorisedRedirectUrl: String = "/not-authorised"
  override val ivUpliftUrl: String = "/iv/uplift"
  override val twoFactorUrl: String = "/two-step-verification/register/"
  override val ggSignInUrl: String = "/gg/sign-in"
  override val ggSignInContinueUrl: String = "/income-tax-subscription-frontend/income-source"
  override val betaFeedbackUnauthenticatedUrl: String = "/unauthorised"
  override val betaFeedbackUrl: String = "/feedback"
  override val contactFormServiceIdentifier: String = "/contact"
  override val contactFrontendPartialBaseUrl: String = "/contact/partial"
  override val alreadyEnrolledUrl: String = "/already-enrolled"
  override val subscriptionUrl: String ="/income-tax-subscription/subscription"
  override val authUrl = ""
}
