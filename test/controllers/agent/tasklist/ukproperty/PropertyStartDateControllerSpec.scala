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

package controllers.agent.tasklist.ukproperty

import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.PropertyStartDateForm
import forms.formatters.DateModelMapping
import models.DateModel
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, POST, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter}
import views.agent.tasklist.ukproperty.mocks.MockPropertyStartDate

import scala.concurrent.Future

class PropertyStartDateControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyStartDate
  with FeatureSwitching
  with GuiceOneAppPerSuite
  with I18nSupport {

  "show" must {
    "return OK with the page content" when {
      "there is no start date already stored" in {
        mockFetchPropertyStartDate(None)
        mockPropertyStartDate(
          postAction = routes.PropertyStartDateController.submit(),
          backUrl = routes.PropertyIncomeSourcesController.show().url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a start date already stored" in {
        mockFetchPropertyStartDate(Some(date))
        mockPropertyStartDate(
          postAction = routes.PropertyStartDateController.submit(),
          backUrl = routes.PropertyIncomeSourcesController.show().url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "in edit mode" in {
        mockFetchPropertyStartDate(None)
        mockPropertyStartDate(
          postAction = routes.PropertyStartDateController.submit(editMode = true),
          backUrl = routes.PropertyIncomeSourcesController.show(editMode = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyStartDateController.show(isEditMode = true, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "in global edit mode" in {
        mockFetchPropertyStartDate(None)
        mockPropertyStartDate(
          postAction = routes.PropertyStartDateController.submit(isGlobalEdit = true),
          backUrl = routes.PropertyIncomeSourcesController.show(isGlobalEdit = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestPropertyStartDateController.show(isEditMode = false, isGlobalEdit = true)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "return BAD_REQUEST with the page contents" when {
      "there was an error produced in the form" when {
        "not in edit mode" in {
          mockPropertyStartDate(
            postAction = routes.PropertyStartDateController.submit(),
            backUrl = routes.PropertyIncomeSourcesController.show().url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in edit mode" in {
          mockPropertyStartDate(
            postAction = routes.PropertyStartDateController.submit(editMode = true),
            backUrl = routes.PropertyIncomeSourcesController.show(editMode = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in global edit mode" in {
          mockPropertyStartDate(
            postAction = routes.PropertyStartDateController.submit(isGlobalEdit = true),
            backUrl = routes.PropertyIncomeSourcesController.show(isGlobalEdit = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = true)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
      }
    }
    "return a redirect to the check your answers" when {
      "a valid date is submitted and it was saves successfully" when {
        "not in edit mode" in {
          mockSavePropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show().url)
        }
        "in edit mode" in {
          mockSavePropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = true, isGlobalEdit = false)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
        }
        "in global edit mode" in {
          mockSavePropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = true)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
        }
      }
    }
    "throw an internal server exception" when {
      "a valid date is submitted but there was a problem saving the date" in {
        mockSavePropertyStartDate(date)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = TestPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
          request
            .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            .withMethod(POST)
            .withFormUrlEncodedBody(
              s"${PropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
              s"${PropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
              s"${PropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
            )
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[PropertyStartDateController][submit] - Could not save start date"
      }
    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatter]
  val appConfig: AppConfig = MockConfig

  object TestPropertyStartDateController extends PropertyStartDateController(
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView,
    implicitDateFormatter
  )

  override def messagesApi: MessagesApi = cc.messagesApi

}
