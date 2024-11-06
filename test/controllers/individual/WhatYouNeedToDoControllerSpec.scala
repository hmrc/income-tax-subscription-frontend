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

import models.{EligibilityStatus, Yes}
import models.status.MandationStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import utilities.agent.TestModels.{testSelectedTaxYearCurrent, testSelectedTaxYearNext}
import views.html.individual.WhatYouNeedToDo
import config.featureswitch.FeatureSwitch.PrePopulate

import scala.concurrent.Future

class WhatYouNeedToDoControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockMandationStatusService
  with MockGetEligibilityStatusService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService
  with MockSessionDataService {

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
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible for both years" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a eligible for next year only" in new Setup {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a mandated current year flag of true" in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any(), any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the session contains a selected software status and tax year " in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
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
    "return SEE_OTHER to the task list page" in new Setup {
      val result: Future[Result] = controller.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)
    }
    "return SEE_OTHER to the your income sources page" in new Setup {
      enable(PrePopulate)
      val result: Future[Result] = controller.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
    }
  }

}
