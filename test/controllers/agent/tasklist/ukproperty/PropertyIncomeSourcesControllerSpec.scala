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

import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.UkPropertyIncomeSourcesForm
import models.common.PropertyModel
import models.{Cash, DateModel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatter
import views.agent.tasklist.ukproperty.mocks.MockPropertyIncomeSources

import scala.concurrent.Future

class PropertyIncomeSourcesControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyIncomeSources
  with GuiceOneAppPerSuite {

  //TODO: Figure out a way to remove the guice app

  "show" should {
    "return OK and display the page" when {
      "there is no stored property data" in {
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
      "there is only a saved start date" in {
        mockFetchProperty(Some(startDateOnlyProperty))
        mockPropertyIncomeSources(
          routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is only a saved accounting method" in {
        mockFetchProperty(Some(accountingMethodOnlyProperty))
        mockPropertyIncomeSources(
          routes.PropertyIncomeSourcesController.submit(),
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyIncomeSourcesController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a complete property saved" in {
        mockFetchProperty(Some(fullProperty))
        mockPropertyIncomeSources(
          routes.PropertyIncomeSourcesController.submit(),
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
          mockSaveProperty(fullProperty)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              UkPropertyIncomeSourcesForm.createPropertyMapData(fullProperty.startDate, fullProperty.accountingMethod).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
        }
        "in edit mode" in {
          mockSaveProperty(fullProperty)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              UkPropertyIncomeSourcesForm.createPropertyMapData(fullProperty.startDate, fullProperty.accountingMethod).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
        }
        "in global edit mode" in {
          mockSaveProperty(fullProperty)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = true)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              UkPropertyIncomeSourcesForm.createPropertyMapData(fullProperty.startDate, fullProperty.accountingMethod).toSeq: _*
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
        }
      }
      "throw an internal server exception" when {
        "there was a problem saving the property business" in {
          mockSaveProperty(fullProperty)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          val result: Future[Result] = TestPropertyIncomeSourcesController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withHeaders("Content-Type" -> "application/x-www-form-urlencoded").withFormUrlEncodedBody(
              UkPropertyIncomeSourcesForm.createPropertyMapData(fullProperty.startDate, fullProperty.accountingMethod).toSeq: _*
            )
          )

          intercept[InternalServerException](await(result))
            .message mustBe "[PropertyIncomeSourcesController][submit] - Could not save property"
        }
      }
    }
  }

  lazy val startDateOnlyProperty: PropertyModel = PropertyModel(startDate = Some(DateModel("1", "1", "1980")))
  lazy val accountingMethodOnlyProperty: PropertyModel = PropertyModel(accountingMethod = Some(Cash))
  lazy val fullProperty: PropertyModel = PropertyModel(startDate = Some(DateModel("1", "1", "1980")), accountingMethod = Some(Cash))

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatter]

  object TestPropertyIncomeSourcesController extends PropertyIncomeSourcesController(
    fakeIdentifierAction,
    implicitDateFormatter,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView
  )

}
