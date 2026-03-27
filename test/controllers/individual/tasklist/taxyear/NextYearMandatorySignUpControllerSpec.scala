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

package controllers.individual.tasklist.taxyear

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingYearForm
import models.common.AccountingYearModel
import models.{AccountingYear, Current, Next}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import views.individual.mocks._
import services.mocks._

import scala.concurrent.Future

class NextYearMandatorySignUpControllerSpec extends ControllerBaseSpec
  with MockNextYearMandatorySignUp
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockGetEligibilityStatusService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockSessionDataService {

  override val controllerName: String = "WhenDoYouWantToStartMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestNextYearMandatorySignUpController.show(isEditMode = false),
    "submit" -> TestNextYearMandatorySignUpController.submit(isEditMode = false)
  )

  object TestNextYearMandatorySignUpController extends NextYearMandatorySignUpController(
    nextYearMandatorySignUp,
    mockAccountingPeriodService,
    mockReferenceRetrieval,
    mockSubscriptionDetailsService,
    mockSessionDataService
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  "show" should {
    "display the nextYearMandatorySignUp page with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

        val result = await(TestNextYearMandatorySignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }

    "display the nextYearMandatorySignUp page with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(None)

        val result = await(TestNextYearMandatorySignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }

  "show" when {
    "the user is allowed to select a tax year" when {
      "they have not previously selected a tax year" should {
        "return OK with the page content" in {
          mockView()
          mockFetchSelectedTaxYear(None)

          val result: Future[Result] = TestNextYearMandatorySignUpController.show(isEditMode = false)(subscriptionRequest)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
      "they have previously selected a tax year" should {
        "return OK with the page content" in {
          mockView()
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

          val result: Future[Result] = TestNextYearMandatorySignUpController.show(isEditMode = false)(subscriptionRequest)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
  }


  "submit" when {

    def callSubmit(isEditMode: Boolean, accountingYear: AccountingYear): Future[Result] = {
      TestNextYearMandatorySignUpController.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(AccountingYearForm.accountingYearForm, accountingYear)
      )
    }

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestNextYearMandatorySignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "in edit mode" should {
      "redirect to the global check your answers page page" in {
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true, accountingYear = Current)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
      }
    }

    "not in edit mode" when {
        "the user signs up for the current tax year" should {
          "redirect to what you need to do page" in {
            mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

            val goodRequest = callSubmit(isEditMode = false, accountingYear = Current)

            status(goodRequest) mustBe SEE_OTHER
            redirectLocation(goodRequest) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
          }
        }
        "the user signs up for the next tax year" should {
          "redirect to the what you need to do page" in {
            mockSaveSelectedTaxYear(AccountingYearModel(Next))(Right(PostSubscriptionDetailsSuccessResponse))

            val goodRequest = callSubmit(isEditMode = false, accountingYear = Next)

            status(goodRequest) mustBe SEE_OTHER
            redirectLocation(goodRequest) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)
          }
        }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        mockView()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)
      }
    }

    "throw an exception" when {
      "there is a failure while saving the tax year" in {
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val goodRequest = callSubmit(isEditMode = false, accountingYear = Current)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

  }
}
