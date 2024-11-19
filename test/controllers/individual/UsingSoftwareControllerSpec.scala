/*
 * Copyright 2024 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitch.PrePopulate
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import forms.individual.UsingSoftwareForm
import models.{EligibilityStatus, No, Yes}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockGetEligibilityStatusService, MockSessionDataService}
import views.html.individual.UsingSoftware
import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockClientDetailsRetrieval
  with MockGetEligibilityStatusService
  with MockSessionDataService {

  object TestUsingSoftwareController extends UsingSoftwareController(
    mock[UsingSoftware],
    mockSessionDataService,
    mockGetEligibilityStatusService
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  trait Setup {
    val usingSoftware: UsingSoftware = mock[UsingSoftware]
    val controller: UsingSoftwareController = new UsingSoftwareController(
      usingSoftware,
      mockSessionDataService,
      mockGetEligibilityStatusService)(
      mockAuditingService,
      mockAuthService,
      appConfig
    )
  }

  override val controllerName: String = "UsingSoftwareController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUsingSoftwareController.show(),
    "submit" -> TestUsingSoftwareController.submit()
  )

  "show" must {
    "return OK with the page content" in new Setup {
      mockFetchSoftwareStatus(Right(None))
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
      when(usingSoftware(
        ArgumentMatchers.eq(UsingSoftwareForm.usingSoftwareForm),
        ArgumentMatchers.eq(routes.UsingSoftwareController.submit())
      )(any(), any())).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show()(
        subscriptionRequest
      )
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" must {

    "redirect to the What You Need To Do page" when {

      "PrePopulate is disabled and the user selects the Yes radio option" in new Setup {
        disable(PrePopulate)
        mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = controller.submit()(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
      }
    }

    "redirect to the What You Need To Do page" when {

      "PrePopulate is disabled and the user selects Yes" in new Setup {
        mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = controller.submit()(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
      }

      "PrePopulate is disabled and the user selects No" in new Setup {
        mockSaveSoftwareStatus(No)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = controller.submit()(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, No))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
      }
    }

    "redirect to the Select Tax Year page" when {
      "PrePopulate is enabled and the user selects Yes" in new Setup {
        enable(PrePopulate)
        mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = controller.submit()(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
      }
    }

    "redirect to the No Software Signpost page" when {
      "PrePopulate is enabled and the user selects No" in new Setup {
        enable(PrePopulate)
        mockSaveSoftwareStatus(No)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = controller.submit()(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, No))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.NoSoftwareController.show.url)
      }
    }

    "throw an exception" when {

      "the user selects invalid option" in new Setup {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        when(usingSoftware(
          any(),
          any()
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.submit()(subscriptionRequest)

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }
}
