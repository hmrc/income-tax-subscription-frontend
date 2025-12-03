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

package controllers.individual

import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import config.featureswitch.FeatureSwitching
import models.Yes.YES
import models.status.MandationStatus.{Mandated, Voluntary}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import utilities.agent.TestModels.{testSelectedTaxYearCurrent, testSelectedTaxYearNext}
import views.html.individual.WhatYouNeedToDo

import scala.concurrent.Future
import _root_.common.Constants.ITSASessionKeys

class WhatYouNeedToDoControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockMandationStatusService
  with MockGetEligibilityStatusService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService
  with MockSessionDataService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EmailCaptureConsent)
  }

  object TestWhatYouNeedToDoController extends WhatYouNeedToDoController(
    mock[WhatYouNeedToDo],
    mockMandationStatusService,
    mockGetEligibilityStatusService,
    mockReferenceRetrieval,
    mockSubscriptionDetailsService,
    mockSessionDataService
  )(
    mockAuditingService,
    appConfig,
    mockAuthService
  )

  trait Setup {
    val whatYouNeedToDo: WhatYouNeedToDo = mock[WhatYouNeedToDo]
    val controller: WhatYouNeedToDoController = new WhatYouNeedToDoController(
      whatYouNeedToDo,
      mockMandationStatusService,
      mockGetEligibilityStatusService,
      mockReferenceRetrieval,
      mockSubscriptionDetailsService,
      mockSessionDataService)(
      mockAuditingService,
      appConfig,
      mockAuthService
    )
  }

  override val controllerName: String = "WhatYouNeedToDoController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYouNeedToDoController.show,
    "submit" -> TestWhatYouNeedToDoController.submit
  )

  "show" must {
    "return OK with the page content" when {
      "the session contains mandated and eligible for only next year" in new Setup {
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
        mockGetAllSessionData(SessionData())
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible for both years" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        mockGetAllSessionData(SessionData())
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible for next year only" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
        mockGetAllSessionData(SessionData())
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a mandated current year flag of true" in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
        )))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a selected software status and tax year " in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        mockGetAllSessionData(SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
        )))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "return SEE_OTHER to the your income sources page" in new Setup {
      val result: Future[Result] = controller.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
    }
  }

  "backUrl" when {
    "the email capture consent feature switch is disabled" should {
      "return the what year to sign up page url" when {
        "the user is eligible for both years, not mandated and selects 'Current' year" in new Setup {
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, None, Some(Current))

          backUrl mustBe controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url
        }
      }
      "return the what tax year to sign up page url" when {
        "the user is eligible for both years, not mandated and selects 'Next' year" in new Setup {
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, None, Some(Next))

          backUrl mustBe controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
      "return the using software page url" when {
        "the user is eligible for next year only" in new Setup {
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = true, mandatedCurrentYear = false, None, None)

          backUrl mustBe controllers.individual.routes.UsingSoftwareController.show().url
        }
        "the user is mandated for the current year" in new Setup {

          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = true, None, None)

          backUrl mustBe controllers.individual.routes.UsingSoftwareController.show().url
        }
      }
    }
    "the email capture consent feature switch is enabled" when {
      "the user is eligible for next year only" should {
        "return the Using Software page url" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = true, mandatedCurrentYear = false, consentStatus = Some(Yes), None)

          backUrl mustBe controllers.individual.routes.UsingSoftwareController.show().url
        }
      }
      "the user is mandated or signing up for current year" should {
        "return the email capture page url when selected Yes to consenting" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, consentStatus = Some(Yes), Some(Current))

          backUrl mustBe controllers.individual.email.routes.EmailCaptureController.show().url
        }
        "return the capture consent page url when selected No to consenting" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, consentStatus = Some(No), Some(Current))

          backUrl mustBe controllers.individual.email.routes.CaptureConsentController.show().url
        }
      }
      "the user is voluntarily signing up for next year" should {
        "return the What Year to Sign Up page url" in new Setup {
          enable(EmailCaptureConsent)
          val backUrl: String = controller.backUrl(eligibleNextYearOnly = false, mandatedCurrentYear = false, consentStatus = None, Some(Next))

          backUrl mustBe controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        }
      }
    }
  }

}
