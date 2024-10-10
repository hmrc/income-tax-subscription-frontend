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

package views.agent.matching

import forms.agent.ClientDetailsForm
import messagelookup.agent.MessageLookup.Base
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.agent.matching.ClientDetails

class ClientDetailsViewSpec extends ViewSpec {

  val clientDetails: ClientDetails = app.injector.instanceOf[ClientDetails]
  val action: Call = ViewSpecTrait.testCall

  val clientDetailsForm: Form[UserDetailsModel] = ClientDetailsForm.clientDetailsForm

  val testError: FormError = FormError("startDate", "agent.error.client-details.date-of-birth.day-month-year.empty")

  private def page(isEditMode: Boolean): HtmlFormat.Appendable = clientDetails(
    clientDetailsForm = ClientDetailsForm.clientDetailsForm,
    postAction = action,
    isEditMode = isEditMode
  )(FakeRequest(), wrappedMessages)

  private def document(editMode: Boolean = false): Document =
    Jsoup.parse(page(isEditMode = editMode).body)

  object ClientDetails {
    val title = "Enter your client’s details"
    val heading = "Enter your client’s details"
    val line1 = "We’ll use these details to check if your client can sign up."
    val field1 = "First name"
    val field2 = "Last name"
    val field3 = "National Insurance number"
    val field4 = "Date of birth"
    val formhint1_line1 = "For example, ‘QQ 12 34 56 C’."
    val formhint2 = "For example, 17 2 1990."
  }

  "The Client Details view" should {
    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = clientDetails(
          ClientDetailsForm.clientDetailsForm,
          testCall,
          isEditMode = false
        ),
        title = ClientDetails.title,
        isAgent = true,
        backLink = None,
      )

      "there is an error" in new TemplateViewTest(
        view = clientDetails(
          ClientDetailsForm.clientDetailsForm.withError(testError),
          testCall,
          isEditMode = false,
        ),
        title = ClientDetails.title,
        isAgent = true,
        backLink = None,
        error = Some(testError)
      )
    }

    "have heading" in {
      document().mainContent.selectHead("h1").text() mustBe ClientDetails.heading
    }

    "have paragraph 1" in {
      document().mainContent.selectHead("p").text() mustBe ClientDetails.line1
    }

    "have a form" which {
      "has the correct attributes" in {
        document().getForm.attr("method") mustBe testCall.method
        document().getForm.attr("action") mustBe testCall.url
      }

      "has a text input to capture client first name" in {
        document().mainContent.mustHaveTextInput(".govuk-form-group:nth-of-type(1)")(
          name = ClientDetailsForm.clientFirstName,
          label = ClientDetails.field1,
          isLabelHidden = false,
          isPageHeading = false
        )
      }

      "has a text input to capture client last name" in {
        document().mainContent.mustHaveTextInput(".govuk-form-group:nth-of-type(2)")(
          name = ClientDetailsForm.clientLastName,
          label = ClientDetails.field2,
          isLabelHidden = false,
          isPageHeading = false
        )
      }

      "has a text input to capture client nino" in {
        document().mainContent.mustHaveTextInput(".govuk-form-group:nth-of-type(3)")(
          name = ClientDetailsForm.clientNino,
          label = ClientDetails.field3,
          isLabelHidden = false,
          isPageHeading = false,
          hint = Some(ClientDetails.formhint1_line1)
        )
      }
    }

    "have a DOB field" in {
      document().mainContent.mustHaveDateInput(
        id = ClientDetailsForm.clientDateOfBirth,
        legend = ClientDetails.field4,
        exampleDate = ClientDetails.formhint2,
        isHeading = false,
        isLegendHidden = false,
        dateInputsValues = Seq(
          DateInputFieldValues("Day", None),
          DateInputFieldValues("Month", None),
          DateInputFieldValues("Year", None)
        )
      )
    }

    "have a continue button" in {
      document().mainContent.selectHead("button").text mustBe Base.continue
    }

    "have a update button" in {
      document(true).mainContent.selectHead("button").text mustBe Base.update
    }
  }
}
