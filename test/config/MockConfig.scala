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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import uk.gov.hmrc.auth.core.ConfidenceLevel
import utilities.UnitTestTrait

trait MockConfig extends UnitTestTrait with AppConfig {

  override val appName: String = "app"
  override val wrongCredentials: String = ""
  override val haveSaUtr: String = ""
  override val btaBaseUrl: String = ""
  override val ggSignInContinueUrl: String = "/income-tax-subscription-frontend"
  override val contactFormServiceIdentifier: String = "MTDIT"
  override val subscriptionUrl: String = "/income-tax-subscription/subscription"
  override val throttlingUrl: String = "/income-tax-subscription/throttled"
  override val mandationStatusUrl: String = "/income-tax-subscription/itsa-status"
  override val getITSAStatusUrl: String = "/income-tax-subscription/get-itsa-status"

  override def prePopUrl(nino: String): String = s"/income-tax-subscription/pre-pop/$nino"

  override val clientMatchingUrl = "/income-tax-subscription/client-matching"
  override val signUpUrl = "/income-tax-subscription/mis/sign-up"
  override val createIncomeSourcesUrl = "/income-tax-subscription/mis/create"
  override val microServiceUrl: String = "income-tax-subscription"
  override val agentClientRelationshipsUrl: String = "/agent-subscription"
  override val preferencesFrontend: String = ""
  override val preferencesFrontendRedirect: String = ""
  override val baseUrl: String = ""
  override val authUrl: String = ""
  override val authenticatorUrl: String = ""
  override val userMatchingUrl = "/income-tax-subscription/user-matching"
  override val ggUrl = ""
  override val urBannerUrl: String = " "
  override val govukGuidanceLink: String = " "
  override val govukGuidanceITSASignUpIndivLink: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
  override val govukGuidanceITSASignUpAgentLink: String = "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-income-tax"
  override val govukGuidanceITSAWhoCanSignUpVoluntarily: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax#who-can-sign-up-voluntarily"

  override def ggSignOutUrl(redirectionUrl: String = ggSignInContinueUrl): String = s"$ggUrl/bas-gateway/sign-out-without-state?continue=$redirectionUrl"

  override val btaUrl = "https://www.tax.service.gov.uk/business-account"
  override val matchingAttempts: Int = 3
  override val matchingLockOutSeconds: Int = 60

  override val identityVerificationRequiredConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L250
  override val identityVerificationURL: String = ""

  override val citizenDetailsURL: String = ""

  override val hasEnabledTestOnlyRoutes: Boolean = false

  override val signUpToSaLink: String = "sa-sign-up"

  override val agentSignUpUrl: String = "/report-quarterly/income-and-expenses/sign-up/client"

  override val agentServicesUrl: String = "/agent-subscription"

  override val agentServicesAccountHomeUrl: String = "/agent-services-account"

  override val backendFeatureSwitchUrl = "income-tax-subscription/"

  override def upsertEnrolmentUrl(enrolmentKey: String): String = "tax-enrolments/"

  override def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String = "tax-enrolments/"

  override val betaFeedbackUnauthenticatedUrl: String = "/feedback"

  override val incomeTaxEligibilityUrl: String = "/income-tax-subscription-incometax.eligibility"

  override val eligibilityFeatureSwitchUrl: String = s"$incomeTaxEligibilityUrl/test-only/feature-switch"

  override val incomeTaxSelfEmploymentsFrontendUrl: String = "/report-quarterly/income-and-expenses/sign-up/self-employments"
  override val incomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = "/report-quarterly/income-and-expenses/sign-up/self-employments/details/business-check-your-answers"
  override val agentIncomeTaxSelfEmploymentsFrontendBusinessCheckYourAnswersUrl: String = "/report-quarterly/income-and-expenses/sign-up/client/self-employments/details/business-check-your-answers"
  override val incomeTaxSelfEmploymentsFrontendInitialiseUrl: String = s"$incomeTaxSelfEmploymentsFrontendUrl/details"
  override val incomeTaxSelfEmploymentsFrontendClientInitialiseUrl: String = s"$incomeTaxSelfEmploymentsFrontendUrl/client/details"

  override lazy val ggLoginUrl: String = "/gg/sign-in"

  override def redirectToLogin(continueUrl: String): play.api.mvc.Result =
    play.api.mvc.Results.Redirect(
      url = ggLoginUrl,
      queryStringParams = Map(
        "continue" -> Seq(continueUrl),
        "origin" -> Seq(appName)
      )
    )
  override def getAllocatedEnrolmentUrl(enrolmentKey: EnrolmentKey): String = ???

  override def queryUsersUrl(utr: String): String = ???

  override def upsertEnrolmentEnrolmentStoreUrl(enrolmentKey: String): String = ???

  override def allocateEnrolmentEnrolmentStoreUrl(groupId: String, enrolmentKey: String): String = ???

  override def assignEnrolmentUrl(userId: String, enrolmentKey: String): String = ???

  override val feedbackFrontendRedirectUrl: String = "/feedback/ITSU"

  override val feedbackFrontendAgentRedirectUrl: String = "/feedback/ITSU-A"

  override val timeoutWarningInSeconds: String = "1234"
  override val timeoutInSeconds: String = "87913"

  override val channelPreferencesUrl: String = "/channel-preferences"

}

object MockConfig extends MockConfig {

  override val configuration: Configuration = mock[Configuration]
  when(configuration.getOptional(any())(any())).thenReturn(None)

}
