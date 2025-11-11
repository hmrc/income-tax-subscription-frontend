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
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import config.featureswitch.FeatureSwitching
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models._
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
import services.mocks._
import views.html.agent.WhatYouNeedToDo
import _root_.common.Constants.ITSASessionKeys
import models.Yes.YES

import scala.concurrent.Future

class AgentWhatYouNeedToDoControllerSpec
  extends ControllerSpec
    with MockIdentifierAction
    with MockConfirmedClientJourneyRefiner
    with MockGetEligibilityStatusService
    with MockMandationStatusService
    with MockSubscriptionDetailsService
    with MockSessionDataService
    with FeatureSwitching {

  val appConfig: AppConfig = MockConfig

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EmailCaptureConsent)
  }

  object TestWhatYouNeedToDoController extends WhatYouNeedToDoController(
    mock[WhatYouNeedToDo],
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockGetEligibilityStatusService,
    mockMandationStatusService,
    mockSubscriptionDetailsService,
    mockSessionDataService
  )(appConfig)

  trait Setup {
    val whatYouNeedToDo: WhatYouNeedToDo = mock[WhatYouNeedToDo]
    val controller: WhatYouNeedToDoController = new WhatYouNeedToDoController(
      whatYouNeedToDo,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner,
      mockGetEligibilityStatusService,
      mockMandationStatusService,
      mockSubscriptionDetailsService,
      mockSessionDataService
    )(appConfig)
  }

  "show" must {
    "return OK with the page content" when {
      "the user is completely voluntary and is eligible for both years" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES),
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url))
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "the user is voluntary but only eligible for next year" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
         mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exceptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show().url)),
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the current year and eligible for all" in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES),
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show().url)),
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the next year and eligible for all" in new Setup {
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES),
          ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
        )))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)),
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "user is eligible for both years" when {
        "user is voluntary" in new Setup {
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(YES),
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)),

          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "user is mandated" in new Setup {

          mockGetMandationService(Mandated, Mandated)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exceptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(YES),
            ITSASessionKeys.CAPTURE_CONSENT -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show().url)),

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
          mockGetMandationService(Voluntary, Voluntary)
           mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exceptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(controllers.agent.routes.UsingSoftwareController.show().url)),
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "user is mandated" in new Setup {

          mockGetMandationService(Voluntary, Mandated)
           mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exceptionReason = None))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          mockGetAllSessionData(SessionData(Map(
            ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
          )))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(controllers.agent.routes.UsingSoftwareController.show().url)),

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
    "the EmailCaptureConsent feature switch is disabled" should {
      "return the What Year To Sign Up page" when {
        "the user is eligible for both years and not mandated for the current year" in new Setup {
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, None, Some(Current))

          backUrl mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
      "return the Using Software page" when {
        "the user is eligible for next year only" in new Setup {
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = true, mandatedCurrentYear = false, None, Some(Next))

          backUrl mustBe controllers.agent.routes.UsingSoftwareController.show().url
        }
        "the user is mandated for the current year" in new Setup {

          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = true, None, Some(Current))

          backUrl mustBe controllers.agent.routes.UsingSoftwareController.show().url
        }
      }
    }
    "the EmailCaptureConsent feature switch is enabled" when {
      "the user is eligible for next year only" should {
        "return the Using Software page" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = true, mandatedCurrentYear = false, captureConsentStatus = Some(Yes), Some(Next))

          backUrl mustBe controllers.agent.routes.UsingSoftwareController.show().url
        }
      }
      "the user is mandated or signing up for current year" should {
        "return the Email Capture page when selected Yes for consent" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = true, captureConsentStatus = Some(Yes), Some(Current))

          backUrl mustBe controllers.agent.email.routes.EmailCaptureController.show().url
        }
        "return the Capture Consent page when selected No for consent" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = true, captureConsentStatus = Some(No), Some(Current))

          backUrl mustBe controllers.agent.email.routes.CaptureConsentController.show().url
        }
      }
      "the user is voluntarily signing up for next year" should {
        "return the What Year to Sign Up page" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, captureConsentStatus = None, Some(Next))

          backUrl mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
      "the user is voluntarily signing up for current year" should {
        "return the What Year to Sign Up page" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, captureConsentStatus = None, Some(Current))

          backUrl mustBe controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
    }
  }
}