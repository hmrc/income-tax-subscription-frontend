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

package views.agent.tasklist.ukproperty

import forms.agent.AccountingMethodPropertyForm
import messagelookup.agent.MessageLookup.{Base => common, PropertyAccountingMethod => messages}
import models.AccountingMethod
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.ukproperty.PropertyAccountingMethod

class UkPropertyAccountingMethodViewSpec extends ViewSpec {

  val testError: FormError = FormError("startDate", "agent.error.accounting-method-property.invalid")
  val propertyAccountingMethod: PropertyAccountingMethod = app.injector.instanceOf[PropertyAccountingMethod]

  class Setup(isEditMode: Boolean = false,
              form: Form[AccountingMethod] = AccountingMethodPropertyForm.accountingMethodPropertyForm) {

    val page: Html = propertyAccountingMethod(
      form,
      testCall,
      isEditMode = isEditMode,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )

    val document: Document = Jsoup.parse(page.body)

  }

  "property accounting method" must {

    "have the correct template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyAccountingMethod(
          accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm,
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = messages.title,
        isAgent = true,
        backLink = Some(testBackUrl)
      )
      "there is an error" in new TemplateViewTest(
        view = propertyAccountingMethod(
          accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.withError(testError),
          postAction = testCall,
          isEditMode = false,
          backUrl = testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = messages.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading" in new Setup {
      document.mainContent.selectHead("h1.govuk-heading-l").text mustBe messages.heading
    }

    "have a caption" in new Setup {
      document.mainContent.selectHead("span.govuk-caption-l").text mustBe messages.caption
    }


    "have a form" which {

      "has a cash radio button" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label").text mustBe messages.cash
      }

      "has a accruals radio button" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label").text mustBe messages.accruals
      }

      "has a save and continue + save and come back later buttons" in new Setup() {
        document.mainContent.selectHead(".govuk-button").text mustBe common.saveAndContinue
        document.mainContent.selectHead(".govuk-button--secondary").text mustBe common.saveAndComeBackLater
      }

    }

  }

}
