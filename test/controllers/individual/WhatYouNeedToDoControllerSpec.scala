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
    "redirect to Accounting Period when selected tax year is Current" in new Setup {
      mockGetAllSessionData(SessionData())
      mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))

      val result: Future[Result] = controller.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url)
    }

    "redirect to Using Software when Next tax year is selected" in new Setup {
      mockGetAllSessionData(SessionData())
      mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))

      val result: Future[Result] = controller.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.routes.UsingSoftwareController.show().url)
    }
  }

  "backUrl" when {
    "return cannot use the service page when eligible for next year only" in new Setup {
      controller.backUrl(eligibleNextYearOnly = true) mustBe controllers.individual.matching.routes.CannotUseServiceController.show().url
    }
    "return what year to sign up page when current tax year" in new Setup {
      controller.backUrl(eligibleNextYearOnly = false) mustBe controllers.individual.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url
    }
  }
}
