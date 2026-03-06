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

package controllers.agent

import config.{AppConfig, MockConfig}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.*
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.*
import views.html.agent.WhatYouNeedToDo
import _root_.common.Constants.ITSASessionKeys
import models.Yes.YES

import scala.concurrent.Future

class AgentWhatYouNeedToDoControllerSpec
  extends ControllerSpec
    with MockIdentifierAction
    with MockConfirmedClientJourneyRefiner
    with MockGetEligibilityStatusService
    with MockSubscriptionDetailsService
    with MockSessionDataService {

  val appConfig: AppConfig = MockConfig

  object TestWhatYouNeedToDoController extends WhatYouNeedToDoController(
    mock[WhatYouNeedToDo],
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockGetEligibilityStatusService,
    mockSubscriptionDetailsService
  )(appConfig)

  trait Setup {
    val whatYouNeedToDo: WhatYouNeedToDo = mock[WhatYouNeedToDo]
    val controller: WhatYouNeedToDoController = new WhatYouNeedToDoController(
      whatYouNeedToDo,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner,
      mockGetEligibilityStatusService,
      mockSubscriptionDetailsService
    )(appConfig)
  }

  "show" must {
    "return OK with the page content" when {
      "the user is completely voluntary and is eligible for both years" in new Setup {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(controllers.agent.email.routes.EmailCaptureController.show().url)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "the user is voluntary but only eligible for next year" in new Setup {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the current year and eligible for all" in new Setup {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(controllers.agent.email.routes.EmailCaptureController.show().url)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the next year and eligible for all" in new Setup {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "user is eligible for both years" when {
        "user is voluntary" in new Setup {
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(controllers.agent.email.routes.EmailCaptureController.show().url)
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "user is mandated" in new Setup {
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(controllers.agent.email.routes.EmailCaptureController.show().url)
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }

      "user is eligible for next year only" when {
        "user is voluntary" in new Setup {
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "user is mandated" in new Setup {
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
  }

  "submit" must {
    "return SEE_OTHER to the Your Income Sources page" in new Setup {
      val result: Future[Result] = controller.submit(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
    }
  }

  "backUrl" when {
    "the user is eligible for next year only" should {
      "return the Cannot sign up this year page" in new Setup {
        val backUrl: String = controller.backUrl(eligibleNextYearOnly = true, captureConsentStatus = Some(Yes), Some(Next))

        backUrl mustBe controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url
      }
    }
    "the user is mandated or signing up for current year" should {
      "return the Email Capture page when selected Yes for consent" in new Setup {
        val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, captureConsentStatus = Some(Yes), Some(Current))

        backUrl mustBe controllers.agent.email.routes.EmailCaptureController.show().url
      }

      "return the Capture Consent page when selected No for consent" in new Setup {
        val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, captureConsentStatus = Some(No), Some(Current))

        backUrl mustBe controllers.agent.email.routes.CaptureConsentController.show().url
      }
    }

    "the user is voluntarily signing up for next year" should {
      "return the What Year to Sign Up page" in new Setup {
        val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, captureConsentStatus = None, Some(Next))

        backUrl mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
      }
    }

    "the user is voluntarily signing up for current year" should {
      "return the What Year to Sign Up page" in new Setup {
        val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, captureConsentStatus = None, Some(Current))

        backUrl mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
      }
    }
  }
}