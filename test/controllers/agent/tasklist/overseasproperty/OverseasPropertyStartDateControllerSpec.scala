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

import config.{AppConfig, MockConfig}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.OverseasPropertyStartDateForm
import forms.formatters.DateModelMapping
import models.DateModel
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter}
import views.agent.mocks.MockOverseasPropertyStartDate
import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching

import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockSubscriptionDetailsService
  with MockOverseasPropertyStartDate
  with FeatureSwitching
  with GuiceOneAppPerSuite
  with I18nSupport {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  "show" must {
    "return OK with the page content" when {
      "there is no start date already stored" in {
        mockFetchOverseasPropertyStartDate(None)
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(),
          backUrl = routes.IncomeSourcesOverseasPropertyController.show().url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestOverseasPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a start date already stored" in {
        mockFetchOverseasPropertyStartDate(Some(date))
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(),
          backUrl = routes.IncomeSourcesOverseasPropertyController.show().url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestOverseasPropertyStartDateController.show(isEditMode = false, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "in edit mode" in {
        mockFetchOverseasPropertyStartDate(None)
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(editMode = true),
          backUrl = routes.IncomeSourcesOverseasPropertyController.show(editMode = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestOverseasPropertyStartDateController.show(isEditMode = true, isGlobalEdit = false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "in global edit mode" in {
        mockFetchOverseasPropertyStartDate(None)
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(isGlobalEdit = true),
          backUrl = routes.IncomeSourcesOverseasPropertyController.show(isGlobalEdit = true).url,
          clientDetails = clientDetails
        )

        val result: Future[Result] = TestOverseasPropertyStartDateController.show(isEditMode = false, isGlobalEdit = true)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "have a backlink" when {
      "remove accounting method feature switch is enabled" in {
        enable(RemoveAccountingMethod)
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(),
          backUrl = routes.OverseasPropertyStartDateBeforeLimitController.show().url,
          clientDetails = clientDetails
        )

        val backUrl = TestOverseasPropertyStartDateController.backUrl(isEditMode = false, isGlobalEdit = false)

        backUrl mustBe routes.OverseasPropertyStartDateBeforeLimitController.show().url

      }
      "remove accounting method feature switch is disabled" in {
        mockOverseasPropertyStartDate(
          postAction = routes.OverseasPropertyStartDateController.submit(),
          backUrl = routes.IncomeSourcesOverseasPropertyController.show().url,
          clientDetails = clientDetails
        )

        val backUrl = TestOverseasPropertyStartDateController.backUrl(isEditMode = false, isGlobalEdit = false)

        backUrl mustBe routes.IncomeSourcesOverseasPropertyController.show().url

      }
    }
  }

  "submit" must {
    "return BAD_REQUEST with the page contents" when {
      "there was an error produced in the form" when {
        "not in edit mode" in {
          mockOverseasPropertyStartDate(
            postAction = routes.OverseasPropertyStartDateController.submit(),
            backUrl = routes.IncomeSourcesOverseasPropertyController.show().url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in edit mode" in {
          mockOverseasPropertyStartDate(
            postAction = routes.OverseasPropertyStartDateController.submit(editMode = true),
            backUrl = routes.IncomeSourcesOverseasPropertyController.show(editMode = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = true, isGlobalEdit = false)(
            request.withMethod("POST").withFormUrlEncodedBody()
          )

          status(result) mustBe BAD_REQUEST
          contentType(result) mustBe Some(HTML)
        }
        "in global edit mode" in {
          mockOverseasPropertyStartDate(
            postAction = routes.OverseasPropertyStartDateController.submit(isGlobalEdit = true),
            backUrl = routes.IncomeSourcesOverseasPropertyController.show(isGlobalEdit = true).url,
            clientDetails = clientDetails
          )

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = true)(
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
          mockSaveOverseasPropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)
        }
        "in edit mode" in {
          mockSaveOverseasPropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = true, isGlobalEdit = false)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
        }
        "in global edit mode" in {
          mockSaveOverseasPropertyStartDate(date)(Right(PostSubscriptionDetailsSuccessResponse))

          val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = true)(
            request
              .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
              .withMethod(POST)
              .withFormUrlEncodedBody(
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
                s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
        }
      }
    }
    "throw an internal server exception" when {
      "a valid date is submitted but there was a problem saving the date" in {
        mockSaveOverseasPropertyStartDate(date)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = TestOverseasPropertyStartDateController.submit(isEditMode = false, isGlobalEdit = false)(
          request
            .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
            .withMethod(POST)
            .withFormUrlEncodedBody(
              s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.day}" -> date.day,
              s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.month}" -> date.month,
              s"${OverseasPropertyStartDateForm.startDate}-${DateModelMapping.year}" -> date.year
            )
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[OverseasPropertyStartDateController][submit] - Could not save start date"
      }
    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatter]
  val appConfig: AppConfig = MockConfig

  object TestOverseasPropertyStartDateController extends OverseasPropertyStartDateController(
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView,
    implicitDateFormatter)(
    appConfig
  )

  override def messagesApi: MessagesApi = cc.messagesApi

}
