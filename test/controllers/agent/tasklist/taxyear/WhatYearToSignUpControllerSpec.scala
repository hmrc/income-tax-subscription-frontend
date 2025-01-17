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

package controllers.agent.tasklist.taxyear

import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.AccountingYearForm
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import models.{Current, EligibilityStatus}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockWhatYearToSignUp

import scala.concurrent.Future

class WhatYearToSignUpControllerSpec extends ControllerSpec
  with MockWhatYearToSignUp
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with FeatureSwitching {

  implicit val appConfig: AppConfig = MockConfig

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    whatYearToSignUp,
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockAccountingPeriodService
  )(appConfig)

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockGetMandationService(Voluntary, Voluntary)

        val result = TestWhatYearToSignUpController.show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(None)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockGetMandationService(Voluntary, Voluntary)

        val result = TestWhatYearToSignUpController.show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should {
    def callSubmit(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      request.withMethod("POST").withFormUrlEncodedBody(AccountingYearForm.accountingYear -> "CurrentYear")
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      request.withMethod("POST").withFormUrlEncodedBody()
    )

    "redirect to global check your answers page" when {
      "in edit mode" in {
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
      }
    }
    "redirect to What You Need To Do page" when {
      "not in edit mode" in {
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        status(goodRequest) must be(Status.SEE_OTHER)
      }
    }

    "return a bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        mockView()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)
      }
    }

    "throw an exception" when {
      "there is a failure while saving the tax year" in {
        mockView()
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val request = callSubmit(isEditMode = false)

        request.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
        intercept[InternalServerException](await(request))
          .message mustBe "[WhatYearToSignUpController][submit] - Could not save accounting year"
      }
    }
  }

  "backUrl" when {
    "in edit mode" must {
      s"return ${controllers.agent.routes.GlobalCheckYourAnswersController.show.url}" in {
        TestWhatYearToSignUpController.backUrl(true) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
      }
    }
    "not in edit mode" must {
      s"return ${controllers.agent.routes.UsingSoftwareController.show.url}" in {
        TestWhatYearToSignUpController.backUrl(false) mustBe Some(controllers.agent.routes.UsingSoftwareController.show.url)
      }
    }
  }
}
