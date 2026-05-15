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

package views.agent.tasklist.selfemployment

import forms.agent.RemoveBusinessForm
import models.YesNo
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import utilities.ViewSpec
import views.html.agent.tasklist.selfemployment.RemoveSelfEmploymentBusiness

class RemoveSelfEmploymentBusinessViewSpec extends ViewSpec {
  private val removeBusinessView = app.injector.instanceOf[RemoveSelfEmploymentBusiness]

  private object RemoveSelfEmploymentBusiness {
    val title = "Delete sole trader business"
    val fullBusinessAndTrade = "Are you sure you want to delete BusyBusiness - Consulting?"
    val withoutBusinessName = "Are you sure you want to delete this business - Consulting?"
    val withoutTradeName = "Are you sure you want to delete BusyBusiness?"
    val withoutNameOrTradeName = "Are you sure you want to delete this business?"
    val paragraph: String = "All of your client’s current sole trader and property businesses need to be added to Making Tax Digital for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val button = "Continue"
  }

  private val businessName = Some("BusyBusiness")
  private val businessTradeName = Some("Consulting")

  private val formError = FormError("startDate", "agent.error.remove-sole-trader-business.invalid")

  "Remove business view" must {
    "have the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = page(),
        title = RemoveSelfEmploymentBusiness.title,
        isAgent = true,
        hasSignOutLink = true
      )

      "there is an error" in new TemplateViewTest(
        view = page(
          form = RemoveBusinessForm.removeBusinessForm().withError(formError)
        ),
        title = RemoveSelfEmploymentBusiness.title,
        isAgent = true,
        hasSignOutLink = true,
        error = Some(formError)
      )
    }

    "have the correct title and subheading" when {
      "there is a business name" when {
        "there is a trade name" in {
          val doc = Jsoup.parse(page().body)

          doc.getH1Element.text() mustBe RemoveSelfEmploymentBusiness.title
          doc.selectHead("legend").text() mustBe RemoveSelfEmploymentBusiness.fullBusinessAndTrade
        }
        "there is no trade name" in {
          val doc = Jsoup.parse(page(maybeBusinessTradeName = None).body)

          doc.getH1Element.text() mustBe RemoveSelfEmploymentBusiness.title
          doc.selectHead("legend").text() mustBe RemoveSelfEmploymentBusiness.withoutTradeName
        }
      }
      "there is no business name" when {
        "there is a trade name" in {
          val doc = Jsoup.parse(page(maybeBusinessName = None).body)

          doc.getH1Element.text() mustBe RemoveSelfEmploymentBusiness.title
          doc.selectHead("legend").text() mustBe RemoveSelfEmploymentBusiness.withoutBusinessName
        }
        "there is no trade name" in {
          val doc = Jsoup.parse(page(maybeBusinessName = None, maybeBusinessTradeName = None).body)

          doc.getH1Element.text() mustBe RemoveSelfEmploymentBusiness.title
          doc.selectHead("legend").text() mustBe RemoveSelfEmploymentBusiness.withoutNameOrTradeName
        }
      }
    }

    "have the correct yes-no radio inputs" in {
      document().mustHaveYesNoRadioInputs(selector = "fieldset")(
        name = RemoveBusinessForm.fieldName,
        legend = RemoveSelfEmploymentBusiness.fullBusinessAndTrade,
        isHeading = false,
        isLegendHidden = false,
        hint = None,
        errorMessage = None,
      )
    }

    "have a paragraph button" in {
      document().mainContent.selectNth("p", 1).text mustBe RemoveSelfEmploymentBusiness.paragraph
    }

    "have a submit button" in {
      document().mainContent.selectHead(".govuk-button").text mustBe RemoveSelfEmploymentBusiness.button
    }
  }

  private def page(
                    form: Form[YesNo] = RemoveBusinessForm.removeBusinessForm(),
                    maybeBusinessName: Option[String] = businessName,
                    maybeBusinessTradeName: Option[String] = businessTradeName
                  )
  = removeBusinessView(form, maybeBusinessName, maybeBusinessTradeName, testCall)

  private def document() =
    Jsoup.parse(page().body)
}
