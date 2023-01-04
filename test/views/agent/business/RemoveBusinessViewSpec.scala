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

package views.agent.business

import forms.agent.RemoveBusinessForm
import models.YesNo
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import utilities.ViewSpec
import views.html.agent.business.RemoveBusiness

class RemoveBusinessViewSpec extends ViewSpec {
  private val removeBusinessView = app.injector.instanceOf[RemoveBusiness]

  private object RemoveBusiness {
    val title = "Are you sure you want to delete BusyBusiness - Consulting?"
    val titleWithoutBusinessName = "Are you sure you want to delete this business - Consulting?"
    val titleWithoutBusinessTradeName = "Are you sure you want to delete BusyBusiness?"
    val titleWithoutNameOrTradeName = "Are you sure you want to delete this business?"
    val paragraph = "All your client’s current sole trader and property businesses need to be added to Making Tax Digital " +
      "for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val yes = "Yes"
    val no = "No"
    val button = "Agree and continue"
  }

  private val maybeBusinessName = Some("BusyBusiness")
  private val maybeBusinessTradeName = Some("Consulting")

  private val formError = FormError("startDate", "agent.error.remove-sole-trader-business.invalid")

  "Remove business view" must {
    "have the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = page(),
        title = RemoveBusiness.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )

      "there is an error" in new TemplateViewTest(
        view = page(
          form = RemoveBusinessForm.removeBusinessForm().withError(formError)
        ),
        title = RemoveBusiness.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true,
        error = Some(formError)
      )
    }

    "have the correct title and heading" when {
      "there is a business name" when {
        "there is a trade name" in {
          val view = page()
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.title,
            isAgent = true
          ).document.getH1Element.text() mustBe RemoveBusiness.title
        }
        "there is no trade name" in {
          val view = page(businessTradeName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutBusinessTradeName,
            isAgent = true
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutBusinessTradeName
        }
      }
      "there is no business name" when {
        "there is a trade name" in {
          val view = page(businessName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutBusinessName,
            isAgent = true
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutBusinessName
        }
        "there is no trade name" in {
          val view = page(businessName = None, businessTradeName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutNameOrTradeName,
            isAgent = true
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutNameOrTradeName
        }
      }
    }

    "have the legend for the page" in {
      document().selectHead("legend").text mustBe RemoveBusiness.title
    }

    "have a hint" in {
      document()
        .selectHead(".govuk-hint")
        .text() mustBe RemoveBusiness.paragraph
    }

    "have a 'yes' radio button" in {
      document()
        .selectNth(".govuk-radios__item", 1)
        .text() mustBe RemoveBusiness.yes
    }

    "have a 'no' radio button" in {
      document()
        .selectNth(".govuk-radios__item", 2)
        .text() mustBe RemoveBusiness.no
    }

    "have a submit button" in {
      document().selectHead("#continue-button").text mustBe RemoveBusiness.button
    }
  }

  private def page(
                    form: Form[YesNo] = RemoveBusinessForm.removeBusinessForm(),
                    businessName: Option[String] = maybeBusinessName,
                    businessTradeName: Option[String] = maybeBusinessTradeName
                  ) =
    removeBusinessView(form, businessName, businessTradeName, testCall, testBackUrl)

  private def document() =
    Jsoup.parse(page().body)
}
