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

package views.individual.tasklist.overseasproperty

import forms.individual.business.ForeignPropertyStartDateBeforeLimitForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.tasklist.overseasproperty.ForeignPropertyStartDateBeforeLimit

class ForeignPropertyStartDateBeforeLimitViewSpec extends ViewSpec {

  "ForeignPropertyStartDateBeforeLimit" must {
    "use the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = view(),
        title = Messages.heading,
        backLink = Some(testBackUrl)
      )
      "there is an error" in new TemplateViewTest(
        view = view(hasError = true),
        title = Messages.heading,
        backLink = Some(testBackUrl),
        error = Some(FormError(
          key = ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit,
          message = "error.individual.foreign-property.start-date-before-limit.invalid",
          args = Seq(AccountingPeriodUtil.getStartDateLimit.getYear.toString)
        ))
      )
    }

    "have a caption for the page section" in {
      document().mainContent.selectHead(".govuk-caption-l").text mustBe Messages.caption
    }

    "have a form" which {
      def form(hasError: Boolean = false): Element = document(hasError).mainContent.getForm

      "has the correct attributes" in {
        form().attr("method") mustBe testCall.method
        form().attr("action") mustBe testCall.url
      }

      "has a yes no radio button question" when {
        "there is no error" in {
          form().mustHaveYesNoRadioInputs("fieldset")(
            name = ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit,
            legend = Messages.heading,
            isHeading = true,
            isLegendHidden = false,
            hint = None,
            errorMessage = None
          )
        }
        "there is an error" in {
          form(hasError = true).mustHaveYesNoRadioInputs("fieldset")(
            name = ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit,
            legend = Messages.heading,
            isHeading = true,
            isLegendHidden = false,
            hint = None,
            errorMessage = Some(Messages.error)
          )
        }
      }

      "has a button group" which {
        def buttonGroup: Element = form().selectHead(".govuk-button-group")

        "has a save and continue button" in {
          buttonGroup.selectHead(".govuk-button").text mustBe Messages.saveAndContinue
        }

        "has a save and come back later button" in {
          val saveAndComeBackLater = buttonGroup.selectHead(".govuk-button--secondary")

          saveAndComeBackLater.text mustBe Messages.saveAndComeBackLater
          saveAndComeBackLater.attr("href") mustBe controllers.individual.tasklist.routes.ProgressSavedController.show(
            location = Some("foreign-property-start-date-before-limit")
          ).url
        }
      }
    }

  }

  lazy val propertyStartDateBeforeLimit: ForeignPropertyStartDateBeforeLimit = app.injector.instanceOf[ForeignPropertyStartDateBeforeLimit]

  def view(hasError: Boolean = false): HtmlFormat.Appendable = propertyStartDateBeforeLimit(
    startDateBeforeLimitForm = if (hasError) {
      ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.bind(Map.empty[String, String])
    } else {
      ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm
    },
    postAction = testCall,
    backUrl = testBackUrl
  )

  def document(hasError: Boolean = false): Element = Jsoup.parse(view(hasError).body)

  object Messages {
    val caption: String = "Your foreign property"
    val heading: String = s"Did you start getting this income before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}?"
    val error: String = s"Select ‘Yes’ if you started getting income from foreign property before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
  }

}
