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

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.AccountingYearForm
import models.Yes.YES
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import models.{AccountingYear, Current, EligibilityStatus, Next, SessionData, Yes}
import play.api.http.Status
import play.api.libs.json.{JsBoolean, JsString}
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
  with MockSessionDataService
  with MockAccountingPeriodService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EmailCaptureConsent)
  }

  implicit val appConfig: AppConfig = MockConfig

  private def testWhatYearToSignUpController(sessionData: SessionData = SessionData()) = new WhatYearToSignUpController(
    whatYearToSignUp,
    fakeIdentifierActionWithSessionData(sessionData),
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockSessionDataService,
    mockAccountingPeriodService
  )(appConfig)

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockGetMandationService(Voluntary, Voluntary)

        val result = testWhatYearToSignUpController().show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in Subscription Details " in {
        mockView()
        mockFetchSelectedTaxYear(None)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockGetMandationService(Voluntary, Voluntary)

        val result = testWhatYearToSignUpController().show(isEditMode = false)(request)

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" when {
    def callSubmit(isEditMode: Boolean, taxYear: AccountingYear = Current, sessionData: SessionData = SessionData()): Future[Result] = testWhatYearToSignUpController(sessionData).submit(isEditMode = isEditMode)(
      request.withMethod("POST").withFormUrlEncodedBody(AccountingYearForm.accountingYear -> taxYear.toString)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = testWhatYearToSignUpController().submit(isEditMode = isEditMode)(
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
        "the email capture consent feature switch is enabled" when {
          "the email passed flag is not present in session" must {
            "save the tax year and redirect to the capture consent page" in {
              enable(EmailCaptureConsent)

              mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

              val result: Future[Result] = callSubmit(isEditMode = false)

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.agent.email.routes.CaptureConsentController.show().url)
            }
          }
          "the email passed flag is present in session" should {
            "save the tax year and redirect to the what you need to do page" in {
              enable(EmailCaptureConsent)

              mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))
              val sessionData = SessionData(Map(
                ITSASessionKeys.HAS_SOFTWARE -> JsBoolean(true),
                ITSASessionKeys.EMAIL_PASSED -> JsBoolean(true)
              ))

              val result: Future[Result] = callSubmit(isEditMode = false, sessionData = sessionData)

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            }
          }
        }
        "the email capture consent feature switch is disabled" must {
          "save the tax year and redirect to the what you need to do page" in {
            mockSaveSelectedTaxYear(AccountingYearModel(Current))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = callSubmit(isEditMode = false)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          }
        }
      }
      "next tax year is selected" must {
        "save the tax year and redirect to the capture consent page" when {
          "the email capture consent feature switch is enabled" in {
            enable(EmailCaptureConsent)

            mockSaveSelectedTaxYear(AccountingYearModel(Next))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = callSubmit(isEditMode = false, taxYear = Next)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          }
          "the email capture consent feature switch is disabled" in {
            mockSaveSelectedTaxYear(AccountingYearModel(Next))(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = callSubmit(isEditMode = false, taxYear = Next)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          }
        }
      }
    }
    "there was a problem saving the tax year selection" must {
      "throw an internal server exception" in {
        mockSaveSelectedTaxYear(AccountingYearModel(Current))(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(callSubmit(isEditMode = false)))
          .message mustBe "[WhatYearToSignUpController][saveSelectedTaxYear] - Could not save accounting year"
      }
    }
  }

  "backUrl" when {
    "in edit mode" must {
      s"return ${controllers.agent.routes.GlobalCheckYourAnswersController.show.url}" in {
        testWhatYearToSignUpController().backUrl(true) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
      }
    }
    "not in edit mode" must {
      s"return ${controllers.agent.routes.UsingSoftwareController.show().url}" in {
        testWhatYearToSignUpController().backUrl(false) mustBe Some(controllers.agent.routes.UsingSoftwareController.show().url)
      }
    }
  }
}
