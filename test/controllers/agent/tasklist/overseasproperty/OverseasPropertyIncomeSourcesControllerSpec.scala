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

package controllers.agent.tasklist.overseasproperty

import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.IncomeSourcesOverseasPropertyForm
import models.common.OverseasPropertyModel
import models.{AccountingMethod, Cash, DateModel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter}
import views.agent.tasklist.overseasproperty.mocks.MockIncomeSourcesOverseasProperty

import scala.concurrent.Future

class OverseasPropertyIncomeSourcesControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockIncomeSourcesOverseasProperty
  with GuiceOneAppPerSuite {

  //TODO: Figure out a way to remove the guice app

  "show" should {
    "return OK and display the page" when {
      "there is no stored overseas property data" in {
        mockFetchOverseasProperty(None)
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved start date which is older than the start date limit" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))))))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved start date which is within the limit" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)))))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a start date before limit answer" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel(startDateBeforeLimit = Some(false))))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved accounting method" in {
        mockFetchOverseasProperty(Some(accountingMethodOnlyOverseasProperty))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a complete overseas property saved" in {
        mockFetchOverseasProperty(Some(fullOverseasProperty))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in edit mode" in {
        mockFetchOverseasProperty(Some(fullOverseasProperty))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = true),
          backUrl = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = true, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in global edit mode" in {
        mockFetchOverseasProperty(Some(fullOverseasProperty))
        mockIncomeSourcesOverseasProperty(
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(isGlobalEdit = true),
          backUrl = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.show(isEditMode = false, isGlobalEdit = true)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" when {
    "an invalid input was submitted" should {
      "return a bad request with the page content" when {
        "not in edit mode" in {
          mockIncomeSourcesOverseasProperty(
            postAction = routes.IncomeSourcesOverseasPropertyController.submit(),
            backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in edit mode" in {
          mockIncomeSourcesOverseasProperty(
            postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = true),
            backUrl = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in global edit mode" in {
          mockIncomeSourcesOverseasProperty(
            postAction = routes.IncomeSourcesOverseasPropertyController.submit(isGlobalEdit = true),
            backUrl = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = true)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
      }
    }
    "they have submitted their start date is before the limit" should {
      "redirect to the property check your answers and save the property" when {
        "not in edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)
        }
        "in edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
        }
        "in global edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = true)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
        }
      }
      "throw an internal server exception" when {
        "there was a problem saving the property business" in {
          mockSaveStreamlineForeignProperty(None, Some(true), accountingMethod)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
            )
          )

          intercept[InternalServerException](await(result))
            .message mustBe "[IncomeSourcesOverseasPropertyController][saveDataAndContinue] - Could not save foreign property income source"
        }
      }
    }
    "they have submitted their start date is not before the limit" should {
      "redirect to the foreign property start date page" when {
        "not in edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty)).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyStartDateController.show().url)
        }
        "in edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty)).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyStartDateController.show(editMode = true).url)
        }
        "in global edit mode" in {
          mockSaveStreamlineForeignProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = true)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty)).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyStartDateController.show(isGlobalEdit = true).url)
        }
      }
      "throw an internal server exception" when {
        "there was a problem saving the property business" in {
          mockSaveStreamlineForeignProperty(None, Some(false), accountingMethod)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          val result: Future[Result] = TestIncomeSourcesOverseasPropertyController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(Some(fullOverseasProperty)).toSeq: _*
            )
          )

          intercept[InternalServerException](await(result))
            .message mustBe "[IncomeSourcesOverseasPropertyController][saveDataAndContinue] - Could not save foreign property income source"
        }
      }
    }
  }

  lazy val startDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)
  lazy val accountingMethod: AccountingMethod = Cash

  lazy val accountingMethodOnlyOverseasProperty: OverseasPropertyModel = OverseasPropertyModel(accountingMethod = Some(Cash))

  lazy val fullOverseasProperty: OverseasPropertyModel = OverseasPropertyModel(
    startDate = Some(startDate),
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(accountingMethod)
  )

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatter]
  val appConfig: AppConfig = MockConfig

  object TestIncomeSourcesOverseasPropertyController extends IncomeSourcesOverseasPropertyController(
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView
  )(appConfig)
}

