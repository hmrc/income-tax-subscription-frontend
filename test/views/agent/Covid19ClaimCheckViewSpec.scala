/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import controllers.agent.eligibility.routes
import forms.agent.Covid19ClaimCheckForm
import org.jsoup.Jsoup
import play.api.data.FormError
import utilities.ViewSpec
import views.html.agent.eligibility.Covid19ClaimCheck

class Covid19ClaimCheckViewSpec extends ViewSpec {
  private val covid19ClaimCheck = app.injector.instanceOf[Covid19ClaimCheck]
  private val action = routes.Covid19ClaimCheckController.submit()

  private val testFormError = FormError(Covid19ClaimCheckForm.fieldName, "test message")

  object Covid19ClaimCheck {
    val title = "Has your client ever claimed a coronavirus (COVID-19) grant or will they in the future?"
    val heading = title
    val openNewTab = "(Opens in new tab)"
    val paragraph1 = "Your client cannot currently join the pilot if they have claimed one or more of these grants (the following links open in a new tab):"
    val paragraph2 = "You can still sign your client up if they’ve only claimed a rebate through the:"
    val bullet1 = s"Self-Employment Support Scheme $openNewTab"
    val bullet2 = s"Coronavirus Job Retention Scheme $openNewTab"
    val bullet3 = s"Coronavirus Statutory Sick Pay Rebate Scheme $openNewTab"
    val bullet4 = s"Test and Trace Support Payment Scheme $openNewTab"
    val bullet5 = s"Local Authority grants $openNewTab"
    val yes = "Yes"
    val no = "No"
    val submitButton = "Continue"
  }

  "Covid19ClaimCheck view" should {
    "display the template correctly" when {
      "there is an error" in new TemplateViewTest(
        view = page(hasError = true),
        title = Covid19ClaimCheck.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(testFormError)
      )

      "there is no error" in new TemplateViewTest(
        view = page(),
        title = Covid19ClaimCheck.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }

    "have the heading for the page" in {
      document().selectHead("h1").text mustBe Covid19ClaimCheck.heading
    }

    "have join pilot paragraph" in {
      document().selectNth(".govuk-body", 1).text mustBe Covid19ClaimCheck.paragraph1
    }

    "have join pilot support scheme point link" in {
      document()
        .selectNth(".govuk-list", 1)
        .selectNth("li", 1)
        .text mustBe Covid19ClaimCheck.bullet1
    }

    "have join pilot retention scheme link" in {
      document()
        .selectNth(".govuk-list", 1)
        .selectNth("li", 2)
        .text mustBe Covid19ClaimCheck.bullet2
    }

    "have still sign your client paragraph" in {
      document().selectNth(".govuk-body", 2).text mustBe Covid19ClaimCheck.paragraph2
    }

    "have claim sick pay link" in {
      document()
        .selectNth(".govuk-list", 2)
        .selectNth("li", 1)
        .text mustBe Covid19ClaimCheck.bullet3
    }

    "have test and trace support pay link" in {
      document()
        .selectNth(".govuk-list", 2)
        .selectNth("li", 2)
        .text mustBe Covid19ClaimCheck.bullet4
    }

    "have local authority grants link" in {
      document()
        .selectNth(".govuk-list", 2)
        .selectNth("li", 3)
        .text mustBe Covid19ClaimCheck.bullet5
    }

    "have a inline radio button" in {
      document()
        .selectHead(".govuk-radios")
        .attr("class")
        .contains("govuk-radios--inline") mustBe true
    }

    "have a yes radio button" in {
      val radioButton = document()
        .selectNth(".govuk-radios__item", 1)

      radioButton.selectHead("label").text mustBe Covid19ClaimCheck.yes
      radioButton.selectHead("input").attr("value") mustBe Covid19ClaimCheck.yes
    }

    "have a no radio button" in {
      val radioButton = document()
        .selectNth(".govuk-radios__item", 2)

      radioButton.selectHead("label").text mustBe Covid19ClaimCheck.no
      radioButton.selectHead("input").attr("value") mustBe Covid19ClaimCheck.no
    }

    "have a continue button" in {
      document().selectHead(".govuk-button").text mustBe Covid19ClaimCheck.submitButton
    }
  }

  private def page(hasError: Boolean = false) = {
    covid19ClaimCheck(
      covid19ClaimCheckForm =
        if(hasError) Covid19ClaimCheckForm.covid19ClaimCheckForm.withError(testFormError)
        else Covid19ClaimCheckForm.covid19ClaimCheckForm,
      postAction = action,
      backUrl = testBackUrl
    )
  }

  private def document() =
    Jsoup.parse(page().body)
}
