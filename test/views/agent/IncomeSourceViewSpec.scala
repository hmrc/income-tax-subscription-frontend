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

import agent.assets.MessageLookup.{IncomeSource => messages}
import forms.agent.IncomeSourceForm
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class IncomeSourceViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean, foreignProperty: Boolean = true): HtmlFormat.Appendable = views.html.agent.income_source(
    incomeSourceForm = IncomeSourceForm.incomeSourceForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode,
    foreignProperty = foreignProperty
  )(FakeRequest(), implicitly, appConfig)


  "The Income Source view" should {
    val testPage = TestView(
      name = "Income Source View",
      title = messages.title,
      heading = messages.heading,
      isAgent = true,
      page = page(isEditMode = false,
        addFormErrors = false
      )
    )

    val form = testPage.getForm("Income Source form")(actionCall = action)

    form.mustHaveCheckboxWithId(
      id = "Business",
      name = "Business",
      message = messages.business
    )
    form.mustHaveCheckboxWithId(
      id = "UkProperty",
      name = "UkProperty",
      message = messages.ukProperty
    )
    form.mustHaveCheckboxWithId(
      id = "ForeignProperty",
      name = "ForeignProperty",
      message = messages.foreignProperty
    )
    form.mustHaveContinueButton()

  }

  "Append Error to the page title if the form has error" should {
    def documentCore(): TestView = TestView(
      name = "Income source View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      isAgent = true,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }

  "Display error summary and error notification" when {
    "foreign property FS is disabled" in {

      lazy val document = Jsoup.parse(page(isEditMode = false, addFormErrors = true, foreignProperty = false).body)

      val errorHeading = document.getElementById("error-summary-heading")
      errorHeading.text() mustBe messages.errorHeading

      val summaryError = document.getElementById("error-summary-heading-href")
      summaryError.text() mustBe messages.errorSummary

      val summaryNotification = document.getElementsByClass("error-notification")
      summaryNotification.text() mustBe s"Error: ${messages.errorSummary}"
    }

    "foreign property FS is enabled" in {
      lazy val document = Jsoup.parse(page(isEditMode = false, addFormErrors = true).body)

      val errorHeading = document.getElementById("error-summary-heading")
      errorHeading.text() mustBe messages.errorHeading

      val summaryError = document.getElementById("error-summary-heading-href")
      summaryError.text() mustBe messages.errorSummaryForeignProperty

      val summaryNotification = document.getElementsByClass("error-notification")
      summaryNotification.text() mustBe s"Error: ${messages.errorSummaryForeignProperty}"
    }
  }

}
