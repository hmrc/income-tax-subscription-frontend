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

package views.individual.incometax.business

import forms.individual.business.UkPropertyCountForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.business.UkPropertyCount

class UkPropertyCountViewSpec extends ViewSpec {

  object UkPropertyCountMessages {
    val captionHidden = "This section is"
    val captionVisible = "UK property"
    val heading: String = "How many properties do you currently let?"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
    val backLink = "Back"
  }

  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("startDate", "error.business.property.count.empty")

  val ukPropertyCount: UkPropertyCount = app.injector.instanceOf[UkPropertyCount]

  "UK property business start" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = ukPropertyCount(
          UkPropertyCountForm.form,
          testCall,
          isEditMode = false,
          testBackUrl
        ),
        title = UkPropertyCountMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
      )

      "there is an error" in new TemplateViewTest(
        view = ukPropertyCount(
          UkPropertyCountForm.form.withError(testError),
          testCall,
          isEditMode = false,
          testBackUrl
        ),
        title = UkPropertyCountMessages.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading with caption" in {
      document().mainContent.selectHead(".hmrc-caption").text mustBe s"${UkPropertyCountMessages.captionHidden} ${UkPropertyCountMessages.captionVisible}"
      document().mainContent.getH1Element.text mustBe UkPropertyCountMessages.heading
    }

    "have a form" which {
      def form = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has a text input field" which {
        "has a hidden label matching the heading" in {
          val label = form.selectHead("label")
          label.text mustBe UkPropertyCountMessages.heading
          label.attr("class") mustBe "govuk-label govuk-visually-hidden"
          label.attr("for") mustBe UkPropertyCountForm.fieldName
        }
        "has a text input" in {
          val input = form.selectHead("input")
          input.attr("type") mustBe "text"
          input.attr("name") mustBe UkPropertyCountForm.fieldName
          input.id mustBe UkPropertyCountForm.fieldName
        }
      }

    }

    "has a save and continue button" in {
      document().mainContent.selectHead(".govuk-button").text mustBe UkPropertyCountMessages.saveAndContinue
    }

    "has a save and come back later button and with a link that redirect to save and retrieve page" in {
      val saveAndComeBackButton: Element = document().mainContent.selectHead(".govuk-button--secondary")
      saveAndComeBackButton.text mustBe UkPropertyCountMessages.saveAndComeBackLater
      saveAndComeBackButton.attr("href") mustBe
        controllers.individual.business.routes.ProgressSavedController.show().url + "?location=uk-property-count"
    }
  }

  private def page(isEditMode: Boolean, ukPropertyCountForm: Form[Int]): Html = {
    ukPropertyCount(
      ukPropertyCountForm,
      testCall,
      isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly)
  }

  private def document(
                        isEditMode: Boolean = false,
                        ukPropertyCountForm: Form[Int] = UkPropertyCountForm.form
                      ): Document = {
    Jsoup.parse(page(isEditMode, ukPropertyCountForm).body)
  }
}
