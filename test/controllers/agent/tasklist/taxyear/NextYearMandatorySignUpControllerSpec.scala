/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent.tasklist.taxyear

import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.AccountingYearForm
import models.*
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.*
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockNextYearMandatorySignUp

import scala.concurrent.Future

class NextYearMandatorySignUpControllerSpec extends ControllerSpec
  with MockNextYearMandatorySignUp
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService {

  implicit val appConfig: AppConfig = MockConfig

  private def testNextYearMandatorySignUpController(sessionData: SessionData = SessionData()) = new NextYearMandatorySignUpController(
    nextYearMandatorySignUp,
    fakeIdentifierActionWithSessionData(sessionData),
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockAccountingPeriodService
  )(appConfig)

  "show" should {
    "display the nextYearMandatorySignUp page with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockGetMandationService(Voluntary, Voluntary)

        val result = testNextYearMandatorySignUpController().show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }

    "display the nextYearMandatorySignUp page with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(None)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        mockGetMandationService(Voluntary, Voluntary)

        val result = testNextYearMandatorySignUpController().show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" when {
    def callSubmit(isEditMode: Boolean, taxYear: AccountingYear = Current, sessionData: SessionData = SessionData()): Future[Result] = testNextYearMandatorySignUpController(sessionData).submit(isEditMode = isEditMode)(
      request.withMethod("POST").withFormUrlEncodedBody(AccountingYearForm.accountingYear -> taxYear.toString)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = testNextYearMandatorySignUpController().submit(isEditMode = isEditMode)(
      request.withMethod("POST").withFormUrlEncodedBody()
    )

    "in edit mode" must {
      "redirect to the global check your answers page" in {
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = callSubmit(isEditMode = true)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
      }
    }
    "not in edit mode" when {
      "no option is selected" must {
        "return a bad request with the page content" in {
          mockView()

          val result: Future[Result] = callSubmitWithErrorForm(isEditMode = false)

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
      }
      "current tax year is selected" when {
        "save the tax year and redirect to the what you need to do page" in {
          mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = callSubmit(isEditMode = false)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        }
      }
      "next tax year is selected" when {
        "save the tax year and redirect to the capture consent page" in {
          mockSaveSelectedTaxYear(AccountingYearModel(Next))(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = callSubmit(isEditMode = false, taxYear = Next)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        }
      }
    }
    "there was a problem saving the tax year selection" must {
      "throw an internal server exception" in {
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(callSubmit(isEditMode = false)))
          .message mustBe "[NextYearMandatorySignUpController][saveSelectedTaxYear] - Could not save accounting year"
      }
    }
  }
}
