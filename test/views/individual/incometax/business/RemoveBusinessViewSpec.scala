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

package views.individual.incometax.business

import forms.individual.business.RemoveBusinessForm
import models.YesNo
import org.jsoup.Jsoup
import play.api.data.{Form, FormError}
import utilities.ViewSpec
import views.html.individual.incometax.business.RemoveBusiness

class RemoveBusinessViewSpec extends ViewSpec {
  private val removeBusinessView = app.injector.instanceOf[RemoveBusiness]

  private object RemoveBusiness {
    val fullTitle = "Are you sure you want to delete BusyBusiness - Consulting?"
    val titleWithoutName = "Are you sure you want to delete this business - Consulting?"
    val titleWithoutTradeName = "Are you sure you want to delete BusyBusiness?"
    val titleWithoutNameOrTradeName = "Are you sure you want to delete this business?"
    val paragraph = "All your current sole trader and property businesses need to be added to Making Tax Digital " +
      "for Income Tax at the same time. You will need to re-enter this information if you remove it by mistake."
    val yes = "Yes"
    val no = "No"
    val button = "Agree and continue"
  }

  private val businessName = Some("BusyBusiness")
  private val businessTradeName = Some("Consulting")

  "Remove business view" must {
    "have the correct template" when {
      "there is no error" when {
        "name and business name are present" in new TemplateViewTest(
          view = page(),
          title = RemoveBusiness.fullTitle,
          backLink = Some(testBackUrl),
        )

      }

      "there is an error" in new TemplateViewTest(
        view = page(
          form = RemoveBusinessForm.removeBusinessForm().withError(FormError("startDate", "testError"))
        ),
        title = RemoveBusiness.fullTitle,
        backLink = Some(testBackUrl),
        error = Some(FormError("startDate", "testError"))
      )
    }

    "have the correct title and heading" when {
      "there is a business name" when {
        "there is a trade name" in {
          val view = page()
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.fullTitle
          ).document.getH1Element.text() mustBe RemoveBusiness.fullTitle
        }
        "there is no trade name" in {
          val view = page(maybeBusinessTradeName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutTradeName
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutTradeName
        }
      }
      "there is no business name" when {
        "there is a trade name" in {
          val view = page(maybeBusinessName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutName
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutName
        }
        "there is no trade name" in {
          val view = page(maybeBusinessName = None, maybeBusinessTradeName = None)
          new TemplateViewTest(
            view = view,
            title = RemoveBusiness.titleWithoutNameOrTradeName
          ).document.getH1Element.text() mustBe RemoveBusiness.titleWithoutNameOrTradeName
        }
      }
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
                    maybeBusinessName: Option[String] = businessName,
                    maybeBusinessTradeName: Option[String] = businessTradeName
                  ) =
    removeBusinessView(form, maybeBusinessName, maybeBusinessTradeName, testCall, testBackUrl)

  private def document() = Jsoup.parse(page().body)
}
