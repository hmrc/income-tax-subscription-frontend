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

package views.agent.tasklist.ukproperty

import config.MockConfig.mockMessages.messages
import forms.agent.UkPropertyIncomeSourcesForm
import messagelookup.agent.MessageLookup.Base.{saveAndComeBackLater, saveAndContinue}
import models.{AccountingMethod, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.ukproperty.PropertyIncomeSources

import java.time.LocalDate

class UkPropertyIncomeSourcesViewSpec extends ViewSpec {

  object UkProppertyIncomeSourcesMessages{
    val title = "Your client’s UK property"
    val heading: String = title
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val para1 = "When did your client’s UK property business start?"
    val dateHint = "For example, 17 8 2014."
    val para2 = "What accounting method does your client use for their UK property business?"
    val radioCash = "Cash basis accounting"
    val radioAccruals = "Traditional accounting"
    val detailsSummary = "Help with accounting method"
    val detailsSubheading = "Example"
    val detailsContentPara = "Your client created an invoice for someone in March 2017, but did not receive the money until May 2017. If your client tells HMRC they received this income in:"
    val detailsBullet1 = "May 2017, they use cash basis accounting"
    val detailsBullet2 =  "March 2017, you use traditional accounting"
  }

  val testErrorStartDate: FormError = FormError("startDate", "agent.error.property.day-month-year.empty")
  val testErrorAccountingMethod: FormError = FormError("accountingMethodProperty", "agent.error.accounting-method-property.invalid")

  val propertyIncomeSourcesForm: Form[(DateModel, AccountingMethod)] = UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm(_.toString)
  val backUrl: String = testBackUrl
  val postAction: Call = testCall

  class Setup(isEditMode: Boolean = false, form: Form[(DateModel, AccountingMethod)] = propertyIncomeSourcesForm) {
    val page: Html = app.injector.instanceOf[PropertyIncomeSources].apply(
      form,
      postAction,
      isEditMode,
      backUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

    val document: Document = Jsoup.parse(page.body)
  }

  "UkPropertyIncomeSourcesView" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = app.injector.instanceOf[PropertyIncomeSources].apply(
          propertyIncomeSourcesForm,
          postAction,
          isEditMode = false,
          backUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = UkProppertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(backUrl)
      )

      "there is an error" in new TemplateViewTest(
        view = app.injector.instanceOf[PropertyIncomeSources].apply(
          propertyIncomeSourcesForm.withError(testErrorStartDate).withError(testErrorAccountingMethod),
          postAction,
          isEditMode = false,
          backUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = UkProppertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(backUrl),
        error = Some(testErrorStartDate)
      )
    }

    "have a heading" in new Setup {
      document.mainContent.selectHead("h1.govuk-heading-l").text mustBe UkProppertyIncomeSourcesMessages.heading
    }

    "have a caption" in new Setup {
      document.mainContent.selectHead("span.govuk-caption-l").text mustBe UkProppertyIncomeSourcesMessages.caption
    }

    "have a form" which {

      "has the correct method and action" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe postAction.method
        form.attr("action") mustBe postAction.url
      }

      "has a date fieldset" in new Setup {
        document.mustHaveGovukDateField("startDate", UkProppertyIncomeSourcesMessages.para1, UkProppertyIncomeSourcesMessages.dateHint)
      }

      "has a radio button fieldset" which {

        "has a cash radio button" in new Setup {
          document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label").text mustBe UkProppertyIncomeSourcesMessages.radioCash
        }

        "has an accruals radio button" in new Setup {
          document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label").text mustBe UkProppertyIncomeSourcesMessages.radioAccruals
        }
      }

      "has a save and continue + save and come back later buttons" in new Setup() {
        document.mainContent.selectHead(".govuk-button").text mustBe saveAndContinue
        document.mainContent.selectHead(".govuk-button--secondary").text mustBe saveAndComeBackLater
      }
    }

    "must display max date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.max-date", List("11 April 2021"))
      val formWithError = propertyIncomeSourcesForm.withError(dateValidationError)

      val doc = new Setup(form = formWithError).document

      doc.mustHaveGovukDateField(
        "startDate",
        UkProppertyIncomeSourcesMessages.para1,
        UkProppertyIncomeSourcesMessages.dateHint,
        Some(messages("agent.error.property.day-month-year.max-date", "11 April 2021"))
      )
    }

    "must display min date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = propertyIncomeSourcesForm.withError(dateValidationError)

      val doc = new Setup(form = formWithError).document

      doc.mustHaveGovukDateField(
        "startDate",
        UkProppertyIncomeSourcesMessages.para1,
        UkProppertyIncomeSourcesMessages.dateHint,
        Some(messages("agent.error.property.day-month-year.min-date", "11 April 2021"))
      )
    }
  }
}

