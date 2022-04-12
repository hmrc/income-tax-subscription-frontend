/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.usermatching

import assets.MessageLookup.{UserDetails => messages}
import forms.usermatching.UserDetailsForm
import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.usermatching.UserDetails

class UserDetailsViewSpec extends ViewSpec {

  val userDetails: UserDetails = app.injector.instanceOf[UserDetails]

  val userDetailsForm: Form[UserDetailsModel] = UserDetailsForm.userDetailsForm.form

  def page(isEditMode: Boolean = false, form: Form[UserDetailsModel] = userDetailsForm): Html = userDetails(
    userDetailsForm = form,
    postAction = testCall,
    isEditMode = isEditMode
  )

  def document(isEditMode: Boolean = false, form: Form[UserDetailsModel] = userDetailsForm): Document = {
    Jsoup.parse(page(isEditMode = isEditMode, form = form).body)
  }

  object UserDetailsMessages {
    val heading = "Confirm your details"
    val line1 = "We will check these details with information we currently have."
    val firstNameLabel = "First name"
    val lastNameLabel = "Last name"
    val ninoLabel = "National Insurance number"
    val dateOfBirthLabel = "Date of birth"
    val ninoHint = "It’s on your National Insurance card, benefit letter, payslip or P60. For example, ‘QQ 12 34 56 C’."
    val dateOfBirthHint = "For example, 10 12 1990"
    val notInPast = "Your date of birth must be in the past"
  }

  val userDetailsFormError: FormError = FormError(UserDetailsForm.userFirstName, "first name error")

  "UserDetails" must {
    "have the correct template" when {
      "the page has no errors" in new TemplateViewTest(
        view = page(),
        title = messages.heading,
        isAgent = false,
      )
      "the page has errors" in new TemplateViewTest(
        view = page(form = userDetailsForm.withError(userDetailsFormError)),
        title = messages.heading,
        isAgent = false,
        error = Some(userDetailsFormError)
      )
    }

    "have a heading" in {
      document().mainContent.getH1Element.text mustBe UserDetailsMessages.heading
    }

    "have a paragraph with information" in {
      document().mainContent.getNthParagraph(1).text mustBe UserDetailsMessages.line1
    }

    "have a a form" which {
      "has the correct attributes" in {
        val form: Element = document().mainContent.getForm

        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }
      "has a first name text field" when {
        "there is no error on the field" in {
          document().mainContent.getForm.selectNth("div", 1).mustHaveTextInput(
            name = UserDetailsForm.userFirstName,
            label = UserDetailsMessages.firstNameLabel,
            autoComplete = Some("given-name")
          )
        }
        "there is an error on the field" in {
          val error: FormError = FormError(UserDetailsForm.userFirstName, "first name error")
          document(form = userDetailsForm.withError(error)).mainContent.getForm.selectNth("div", 1).mustHaveTextInput(
            name = UserDetailsForm.userFirstName,
            label = UserDetailsMessages.firstNameLabel,
            error = Some(error),
            autoComplete = Some("given-name")
          )
        }
      }
      "has a last name text field" when {
        "there is no error on the field" in {
          document().mainContent.getForm.selectNth("div", 2).mustHaveTextInput(
            name = UserDetailsForm.userLastName,
            label = UserDetailsMessages.lastNameLabel,
            autoComplete = Some("family-name")
          )
        }
        "there is an error on the field" in {
          val error: FormError = FormError(UserDetailsForm.userLastName, "last name error")
          document(form = userDetailsForm.withError(error)).mainContent.getForm.selectNth("div", 2).mustHaveTextInput(
            name = UserDetailsForm.userLastName,
            label = UserDetailsMessages.lastNameLabel,
            error = Some(error),
            autoComplete = Some("family-name")
          )
        }
      }
      "has a national insurance number text field" when {
        "there is no error on the field" in {
          document().mainContent.getForm.selectNth("div", 3).mustHaveTextInput(
            name = UserDetailsForm.userNino,
            label = UserDetailsMessages.ninoLabel,
            hint = Some(UserDetailsMessages.ninoHint)
          )
        }
        "there is an error on the field" in {
          val error: FormError = FormError(UserDetailsForm.userNino, "nino error")
          document(form = userDetailsForm.withError(error)).mainContent.getForm.selectNth("div", 3).mustHaveTextInput(
            name = UserDetailsForm.userNino,
            label = UserDetailsMessages.ninoLabel,
            hint = Some(UserDetailsMessages.ninoHint),
            error = Some(error)
          )
        }
      }
      "has date of birth fields" when {
        "there are no errors" in {
          document().mainContent.getForm.selectNth("div", 4).mustHaveDateInput(
            name = UserDetailsForm.userDateOfBirth,
            label = UserDetailsMessages.dateOfBirthLabel,
            hint = Some(UserDetailsMessages.dateOfBirthHint),
            isDateOfBirth = true
          )
        }
        "there is an error field" in {
          val error: FormError = FormError(UserDetailsForm.userDateOfBirth, "error.user_details.date_of_birth.day_month_year.not_in_past")
          document(form = userDetailsForm.withError(error))
            .mainContent.selectHead(".govuk-error-summary__list li").text() mustBe UserDetailsMessages.notInPast
        }
      }
      "has a button to submit the form" which {
        "says continue when not in edit mode" in {
          document().mainContent.getForm.selectHead("button").text mustBe "Continue"
        }
        "says update when in edit mode" in {
          document(isEditMode = true).mainContent.getForm.selectHead("button").text mustBe "Update"
        }
      }
    }

  }

}
