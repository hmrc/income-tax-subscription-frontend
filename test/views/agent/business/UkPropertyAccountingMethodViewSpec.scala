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

package views.agent.business

import agent.assets.MessageLookup.{Base => common, PropertyAccountingMethod => messages}
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import forms.agent.AccountingMethodPropertyForm
import models.AccountingMethod
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.business.PropertyAccountingMethod

class UkPropertyAccountingMethodViewSpec extends ViewSpec {

  val testError: FormError = FormError("startDate", "testError")

  val propertyAccountingMethod: PropertyAccountingMethod = app.injector.instanceOf[PropertyAccountingMethod]

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SaveAndRetrieve)
  }

  class Setup(isEditMode: Boolean = false,
              form: Form[AccountingMethod] = AccountingMethodPropertyForm.accountingMethodPropertyForm,
              enableSaveAndRetrieve: Boolean = false) {

    if (enableSaveAndRetrieve) {
      enable(SaveAndRetrieve)
    }

    val page: Html = propertyAccountingMethod(
      form,
      testCall,
      isEditMode = isEditMode,
      testBackUrl
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
          backUrl = testBackUrl
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
          backUrl = testBackUrl
        ),
        title = messages.title,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a form" which {

      "has a cash radio button" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label").text mustBe messages.cash
      }

      "has a accruals radio button" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label").text mustBe messages.accruals
      }

      "has a save and continue + save and come back later buttons" when {
        "the save and retrieve feature switch is enabled" in new Setup(enableSaveAndRetrieve = true) {
          document.mainContent.selectHead(".govuk-button").text mustBe common.saveAndContinue
          document.mainContent.selectHead(".govuk-button--secondary").text mustBe common.saveAndComeBackLater
        }
      }

      "has a continue button" when {
        "the save and retrieve feature switch is disabled" that {
          s"displays ${common.continue} when not in edit mode" in new Setup {
            document.select(".govuk-button").text mustBe common.continue
          }
          s"displays ${common.update} when in edit mode" in new Setup(isEditMode = true) {
            document.select(".govuk-button").text mustBe common.update
          }
        }
      }

    }

  }

}
