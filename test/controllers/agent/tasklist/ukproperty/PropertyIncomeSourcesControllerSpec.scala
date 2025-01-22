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

package controllers.agent.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.UkPropertyIncomeSourcesForm
import models.common.PropertyModel
import models.{AccountingMethod, Cash, DateModel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter}
import views.agent.tasklist.ukproperty.mocks.MockPropertyIncomeSources

import scala.concurrent.Future

class PropertyIncomeSourcesControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyIncomeSources
  with FeatureSwitching
  with GuiceOneAppPerSuite {

  override def beforeEach(): Unit = {
    disable(StartDateBeforeLimit)
    super.beforeEach()
  }

  //TODO: Figure out a way to remove the guice app

  "show" when {
    "return OK and display the page" when {
      "there is no stored property data" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(None)
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved start date which is older than the start date limit" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(Some(PropertyModel(startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))))))
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved start date which is within the limit" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(Some(PropertyModel(startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)))))
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a start date before limit answer" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(Some(PropertyModel(startDateBeforeLimit = Some(false))))
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only an accounting method" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a complete property" in {
        enable(StartDateBeforeLimit)

        mockFetchProperty(Some(fullProperty))
        mockPropertyIncomeSources(
          postAction = routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in edit mode" in {
        mockFetchProperty(Some(fullProperty))
        mockPropertyIncomeSources(
          routes.PropertyIncomeSourcesController.submit(editMode = true),
          backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = true, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the page is in global edit mode" in {
        mockFetchProperty(Some(fullProperty))
        mockPropertyIncomeSources(
          routes.PropertyIncomeSourcesController.submit(isGlobalEdit = true),
          backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = true)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" when {
    "the start date before limit feature switch is enabled" should {
      "an invalid input was submitted" should {
        "return a bad request with the page content" when {
          "not in edit mode" in {
            enable(StartDateBeforeLimit)

            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(),
              backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            )

            status(result) mustBe BAD_REQUEST
            contentType(result) mustBe Some(HTML)
          }
          "in edit mode" in {
            enable(StartDateBeforeLimit)

            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(editMode = true),
              backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            )

            status(result) mustBe BAD_REQUEST
            contentType(result) mustBe Some(HTML)
          }
          "in global edit mode" in {
            enable(StartDateBeforeLimit)

            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(isGlobalEdit = true),
              backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
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
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
          }
          "in edit mode" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
          }
          "in global edit mode" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(true), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
          }
        }
        "throw an internal server exception" when {
          "there was a problem saving the property business" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(true), accountingMethod)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty.copy(startDateBeforeLimit = Some(true)))).toSeq: _*
              )
            )

            intercept[InternalServerException](await(result))
              .message mustBe "[PropertyIncomeSourcesController][saveDataAndContinue] - Could not save property income source"
          }
        }
      }
      "they have submitted their start date is not before the limit" should {
        "redirect to the property start date page" when {
          "not in edit mode" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyStartDateController.show().url)
          }
          "in edit mode" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyStartDateController.show(editMode = true).url)
          }
          "in global edit mode" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(false), accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyStartDateController.show(isGlobalEdit = true).url)
          }
        }
        "throw an internal server exception" when {
          "there was a problem saving the property business" in {
            enable(StartDateBeforeLimit)

            mockSaveStreamlineProperty(None, Some(false), accountingMethod)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            intercept[InternalServerException](await(result))
              .message mustBe "[PropertyIncomeSourcesController][saveDataAndContinue] - Could not save property income source"
          }
        }
      }
    }
    "the start date before limit feature switch is disabled" should {
      "an invalid input was submitted" should {
        "return a bad request with the page content" when {
          "not in edit mode" in {
            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(),
              backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            )

            status(result) mustBe BAD_REQUEST
            contentType(result) mustBe Some(HTML)
          }
          "in edit mode" in {
            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(editMode = true),
              backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(editMode = true).url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            )

            status(result) mustBe BAD_REQUEST
            contentType(result) mustBe Some(HTML)
          }
          "in global edit mode" in {
            mockPropertyIncomeSources(
              routes.PropertyIncomeSourcesController.submit(isGlobalEdit = true),
              backUrl = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url,
              clientDetails = clientDetails
            )

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            )

            status(result) mustBe BAD_REQUEST
            contentType(result) mustBe Some(HTML)
          }
        }
      }
      "complete and valid input is submitted" should {
        "redirect to the property check your answers and save the property" when {
          "not in edit mode" in {
            mockSaveStreamlineProperty(Some(startDate), None, accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
          }
          "in edit mode" in {
            mockSaveStreamlineProperty(Some(startDate), None, accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
          }
          "in global edit mode" in {
            mockSaveStreamlineProperty(Some(startDate), None, accountingMethod)(Right(PostSubscriptionDetailsSuccessResponse))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
          }
        }
        "throw an internal server exception" when {
          "there was a problem saving the property business" in {
            mockSaveStreamlineProperty(Some(startDate), None, accountingMethod)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

            val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
              request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
                UkPropertyIncomeSourcesForm.createPropertyMapData(Some(fullProperty)).toSeq: _*
              )
            )

            intercept[InternalServerException](await(result))
              .message mustBe "[PropertyIncomeSourcesController][saveDataAndContinue] - Could not save property income source"
          }
        }
      }
    }
  }

  lazy val startDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)
  lazy val accountingMethod: AccountingMethod = Cash

  lazy val fullProperty: PropertyModel = PropertyModel(
    startDate = Some(startDate),
    startDateBeforeLimit = Some(false),
    accountingMethod = Some(accountingMethod)
  )

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatter]
  val appConfig: AppConfig = MockConfig

  object TestPropertyIncomeSourcesController extends PropertyIncomeSourcesController(
    fakeIdentifierAction,
    implicitDateFormatter,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView
  )(appConfig)

}
