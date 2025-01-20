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

import config.MockConfig
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{Current, EligibilityStatus, Next, Yes}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import utilities.agent.TestConstants.{testFormattedNino, testName}
import views.html.agent.WhatYouNeedToDo

import scala.concurrent.Future

class AgentWhatYouNeedToDoControllerSpec
  extends ControllerSpec
    with MockIdentifierAction
    with MockConfirmedClientJourneyRefiner
    with MockGetEligibilityStatusService
    with MockMandationStatusService
    with MockSubscriptionDetailsService
    with MockSessionDataService {

  val appConfig = MockConfig

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
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockFetchSoftwareStatus(Right(Some(Yes)))

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
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
        mockFetchSoftwareStatus(Right(Some(Yes)))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show.url)),
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the current year and eligible for all" in new Setup {
        mockGetMandationService(Mandated, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockFetchSoftwareStatus(Right(Some(Yes)))

        when(whatYouNeedToDo(
          ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.eq(clientDetails.name),
          ArgumentMatchers.eq(clientDetails.formattedNino),
          ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show.url)),
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          request
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the next year and eligible for all" in new Setup {
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
        mockFetchSoftwareStatus(Right(Some(Yes)))

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
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockFetchSoftwareStatus(Right(Some(Yes)))

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
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          mockFetchSoftwareStatus(Right(Some(Yes)))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(routes.UsingSoftwareController.show.url)),

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
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          mockFetchSoftwareStatus(Right(Some(Yes)))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(controllers.agent.routes.UsingSoftwareController.show.url)),
          )(any(), any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = controller.show(
            request
          )

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "user is mandated" in new Setup {

          mockGetMandationService(Voluntary, Mandated)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          mockFetchSoftwareStatus(Right(Some(Yes)))

          when(whatYouNeedToDo(
            ArgumentMatchers.eq(routes.WhatYouNeedToDoController.submit),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.any(),
            ArgumentMatchers.eq(clientDetails.name),
            ArgumentMatchers.eq(clientDetails.formattedNino),
            ArgumentMatchers.eq(Some(controllers.agent.routes.UsingSoftwareController.show.url)),

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

}