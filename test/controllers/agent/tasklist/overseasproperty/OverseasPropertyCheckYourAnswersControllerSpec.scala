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

package controllers.agent.tasklist.overseasproperty

import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.agent.tasklist.overseasproperty.mocks.MockOverseasPropertyCheckYourAnswers

import scala.concurrent.Future

class OverseasPropertyCheckYourAnswersControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockOverseasPropertyCheckYourAnswers
  with FeatureSwitching {

  "show" when {
    "no property data was returned" should {
      "throw an exception" in {
        mockFetchOverseasProperty(None)

        val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)

        intercept[InternalServerException](await(result))
          .message mustBe "[OverseasPropertyCheckYourAnswersController] - Could not retrieve overseas property details"
      }
    }
    "property data was returned" should {
      "return OK with the page contents" when {
        "not in edit mode" in {
            mockFetchOverseasProperty(Some(fullOverseasProperty))
            mockOverseasPropertyCheckYourAnswers(
              viewModel = fullOverseasProperty,
              postAction = routes.OverseasPropertyCheckYourAnswersController.submit(),
              isGlobalEdit = false,
              backUrl = routes.IncomeSourcesOverseasPropertyController.show().url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        "in edit mode" in {
          mockFetchOverseasProperty(Some(fullOverseasProperty))
          mockOverseasPropertyCheckYourAnswers(
            viewModel = fullOverseasProperty,
            postAction = routes.OverseasPropertyCheckYourAnswersController.submit(),
            isGlobalEdit = false,
            backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.show(isEditMode = true, isGlobalEdit = false)(request)

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "in global edit mode" when {
          "the property business is confirmed" in {
            mockFetchOverseasProperty(Some(fullOverseasProperty.copy(confirmed = true)))
            mockOverseasPropertyCheckYourAnswers(
              viewModel = fullOverseasProperty.copy(confirmed = true),
              postAction = routes.OverseasPropertyCheckYourAnswersController.submit(true),
              isGlobalEdit = true,
              backUrl = controllers.agent.routes.GlobalCheckYourAnswersController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "the property business is not confirmed" in {
            mockFetchOverseasProperty(Some(fullOverseasProperty))
            mockOverseasPropertyCheckYourAnswers(
              viewModel = fullOverseasProperty,
              postAction = routes.OverseasPropertyCheckYourAnswersController.submit(true),
              isGlobalEdit = true,
              backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = true)(request)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        }
      }
    }
  }

  "submit" when {
    "no property data was returned" should {
      "throw an exception" in {
        mockFetchOverseasProperty(None)

        val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

        intercept[InternalServerException](await(result))
          .message mustBe "[OverseasPropertyCheckYourAnswersController] - Could not retrieve overseas property details"
      }
    }
    "property data was returned" which {
      "is missing the accounting method" should {
        "redirect to the your income sources page" in {
          disable(RemoveAccountingMethod)
          mockFetchOverseasProperty(Some(fullOverseasProperty.copy(accountingMethod = None)))

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
      "is missing the start date" should {
        "redirect to the your income sources page" in {
          mockFetchOverseasProperty(Some(fullOverseasProperty.copy(startDate = None)))

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
    }
    "property data was returned and is complete" should {
      "save the property as confirmed and redirect to the global check your answers" when {
        "in global edit mode" in {
          mockFetchOverseasProperty(Some(fullOverseasProperty))
          mockSaveOverseasProperty(fullOverseasProperty.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = true)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
        }
      }
      "save the property as confirmed and redirect to the your income sources" when {
        "not in edit mode" in {
          mockFetchOverseasProperty(Some(fullOverseasProperty))
          mockSaveOverseasProperty(fullOverseasProperty.copy(confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
      "throw an exception" when {
        "a failure occured when saving the confirmed property" in {
          mockFetchOverseasProperty(Some(fullOverseasProperty))
          mockSaveOverseasProperty(fullOverseasProperty.copy(confirmed = true))(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          val result: Future[Result] = TestOverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = false)(request)

          intercept[InternalServerException](await(result))
            .message mustBe "[OverseasPropertyCheckYourAnswersController][submit] - Could not confirm overseas property"
        }
      }
    }
  }

  val appConfig: AppConfig = mock[AppConfig]

  lazy val fullOverseasProperty: OverseasPropertyModel = OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("1", "1", "1980")))

  object TestOverseasPropertyCheckYourAnswersController extends OverseasPropertyCheckYourAnswersController(
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView,
    MockConfig
  )

}