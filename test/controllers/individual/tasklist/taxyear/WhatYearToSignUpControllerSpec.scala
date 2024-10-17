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

package controllers.individual.tasklist.taxyear

import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingYearForm
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import models.{Current, EligibilityStatus}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks._

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends ControllerBaseSpec
  with MockWhatYearToSignUp
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockGetEligibilityStatusService
  with MockReferenceRetrieval
  with MockAuditingService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  override val controllerName: String = "WhatYearToSignUpMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYearToSignUpController.show(isEditMode = false),
    "submit" -> TestWhatYearToSignUpController.submit(isEditMode = false)
  )

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    whatYearToSignUp,
    mockAccountingPeriodService,
    mockReferenceRetrieval,
    mockSubscriptionDetailsService
  )(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockGetEligibilityStatusService,
    mockMandationStatusService,
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

        val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)

      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))

        mockView()
        mockFetchSelectedTaxYear(None)

        val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        status(result) must be(Status.OK)
      }
    }
  }


  "submit" when {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, Current)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "prepopulate feature switch is enabled" should {

      "redirect to global CYA when in edit mode" in {
        enable(PrePopulate)
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)

        await(goodRequest)
      }


      "redirect to What You Need to Do page when not in edit mode" in {
        enable(PrePopulate)
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.routes.WhatYouNeedToDoController.show.url)

        await(goodRequest)
      }
    }

    "prepopulate feature switch is disabled" should {

      "redirect to taxYearCYA page" when {
        "not in edit mode" in {
          mockView()
          mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

          val goodRequest = callSubmit(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show().url)

          await(goodRequest)
        }

        "in edit mode" in {
          mockView()
          mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

          val goodRequest = callSubmit(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show().url)

          await(goodRequest)
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

        val goodRequest = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url" when {

      "prepopulate feature switch is enabled" should {

        "redirect the user to the Global CYA page when in edit mode" in {
          enable(PrePopulate)
          mockView()
          TestWhatYearToSignUpController.backUrl(isEditMode = true) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)
        }

        "redirect the user to the Using Software page when in edit mode" in {
          enable(PrePopulate)
          mockView()
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe Some(controllers.individual.routes.UsingSoftwareController.show().url)
        }
      }

      "prepopulate feature switch is disabled" should {

        "return the user to the task list page" in {
          mockView()
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)
        }

        "return the user to the taxYearCYA page when is in editMode" in {
          mockView()
          TestWhatYearToSignUpController.backUrl(isEditMode = true) mustBe Some(controllers.individual.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(true).url)
        }
      }
    }

  }
}
