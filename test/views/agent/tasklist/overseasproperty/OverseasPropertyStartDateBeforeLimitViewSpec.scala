/*
 * Copyright 2025 HM Revenue & Customs
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

package views.agent.tasklist.overseasproperty

import forms.agent.OverseasPropertyStartDateBeforeLimitForm
import forms.agent.OverseasPropertyStartDateBeforeLimitForm.overseasPropertyStartDateBeforeLimitForm
import forms.submapping.YesNoMapping
import messagelookup.agent.MessageLookup.Base.{saveAndComeBackLater, saveAndContinue}
import models.YesNo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDateBeforeLimit

class OverseasPropertyStartDateBeforeLimitViewSpec extends ViewSpec {

  "PropertyStartDateBeforeLimit view" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = view.apply(
          overseasPropertyStartDateBeforeLimitForm,
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasPropertyStartDateBeforeLimitMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl)
      )

      "there is an error" in new TemplateViewTest(
        view = view.apply(
          ukPropertyStartDateBeforeLimitForm.withError(testError),
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasPropertyStartDateBeforeLimitMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading and caption" in new Setup {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyStartDateBeforeLimitMessages.heading,
        caption = OverseasPropertyStartDateBeforeLimitMessages.caption,
        isSection = false
      )
    }

    "have a form" which {

      "has the correct method and action" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "have a yes no radio input for the start date before limit field" in new Setup {
        document.mainContent.selectNth(".govuk-form-group", 1).mustHaveRadioInput(selector = ".govuk-form-group")(
          name = OverseasPropertyStartDateBeforeLimitForm.startDateBeforeLimit,
          legend = OverseasPropertyStartDateBeforeLimitMessages.dateBeforeLimitLegend,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(OverseasPropertyStartDateBeforeLimitMessages.yes),
              value = Some(YesNoMapping.option_yes)
            ),
            RadioItem(
              content = Text(OverseasPropertyStartDateBeforeLimitMessages.no),
              value = Some(YesNoMapping.option_no)
            )
          ),
          isInline = true
        )
      }

      "has a save and continue + save and come back later buttons" in new Setup() {
        document.mainContent.selectHead(".govuk-button").text mustBe saveAndContinue
        document.mainContent.selectHead(".govuk-button--secondary").text mustBe saveAndComeBackLater
      }
    }

  }

  object OverseasPropertyStartDateBeforeLimitMessages {
    val title = "Your client’s foreign property"
    val heading: String = title
    val caption = "FirstName LastName – ZZ 11 11 11 Z"
    val dateBeforeLimitLegend = s"Did this income start before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}?"
    val yes = "Yes"
    val no = "No"
  }

  lazy val testError: FormError = FormError("start-date-before-limit", "agent.error.property.empty")

  lazy val ukPropertyStartDateBeforeLimitForm: Form[YesNo] = OverseasPropertyStartDateBeforeLimitForm.overseasPropertyStartDateBeforeLimitForm

  lazy val view: OverseasPropertyStartDateBeforeLimit = app.injector.instanceOf[OverseasPropertyStartDateBeforeLimit]

  class Setup() {
    def page: Html = view(
      ukPropertyStartDateBeforeLimitForm,
      testCall,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

    def document: Document = Jsoup.parse(page.body)
  }

}
