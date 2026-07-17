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

package controllers.individual

import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.individual.actions.mocks.MockSignUpJourneyRefiner
import forms.individual.UsingSoftwareForm
import forms.submapping.YesNoMapping
import models.{No, Yes}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.UsingSoftware

import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerSpec
  with MockSignUpJourneyRefiner {

  trait Setup {
    val usingSoftware: UsingSoftware = mock[UsingSoftware]
    val controller: UsingSoftwareController = new UsingSoftwareController(
      fakeIdentifierAction,
      fakeSignUpJourneyRefiner,
      usingSoftware,
      mockSessionDataService
    )
  }


  "show" must {
    "return OK with the page content" in new Setup {
      when(usingSoftware(
        ArgumentMatchers.eq(UsingSoftwareForm.usingSoftwareForm),
        ArgumentMatchers.eq(routes.UsingSoftwareController.submit())
      )(any(), any())).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = controller.show(false)(request)
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" when {
    "the users submission is invalid" should {
      "return a bad request with the page content" in new Setup {
        when(usingSoftware(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.submit(false)(request.withMethod("POST"))

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
    "the user submits 'Yes'" should {
      "redirect to the your income sources page" when {
        "the session save succeeds" in new Setup {
          mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

          val result: Future[Result] = controller.submit(false)(
            request
              .withMethod("POST")
              .withFormUrlEncodedBody(UsingSoftwareForm.fieldName -> YesNoMapping.option_yes)
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
      "respect edit mode when redirecting" when {
        "the user answers yes or no" in new Setup {
          Seq(Yes, No).foreach { answer =>
            mockSaveSoftwareStatus(answer)(Right(SaveSessionDataSuccessResponse))

            Seq(false, true).foreach { editMode =>
              val result: Future[Result] = controller.submit(editMode)(request.withMethod("POST").withFormUrlEncodedBody(
                UsingSoftwareForm.fieldName -> (answer match {
                  case Yes => YesNoMapping.option_yes
                  case No => YesNoMapping.option_no
                })
              ))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(
                if (editMode) controllers.individual.routes.GlobalCheckYourAnswersController.show.url
                else controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
              )
            }
          }
        }
      }
    }
    "an error occurs when saving the software status" should {
      "throw an internal server exception" in new Setup {
        mockSaveSoftwareStatus(Yes)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = controller.submit(false)(
          request
            .withMethod("POST")
            .withFormUrlEncodedBody(UsingSoftwareForm.fieldName -> YesNoMapping.option_yes)
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[UsingSoftwareController][submit] - Could not save using software answer"
      }
    }
  }

}
