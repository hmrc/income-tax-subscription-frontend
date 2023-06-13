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

import forms.agent.AccountingMethodOverseasPropertyForm
import forms.submapping.AccountingMethodMapping
import models.AccountingMethod
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.business.OverseasPropertyAccountingMethod

class OverseasPropertyAccountingMethodViewSpec extends ViewSpec {

  val overseasPropertyAccountingMethod: OverseasPropertyAccountingMethod = app.injector.instanceOf[OverseasPropertyAccountingMethod]
  val testError: FormError = FormError(AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty, "error.agent.overseas-property-accounting-method.empty")

  def page(form: Form[AccountingMethod] = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm,
           isEditMode: Boolean = false): Html = {
    overseasPropertyAccountingMethod(
      accountingMethodOverseasPropertyForm = form,
      postAction = testCall,
      isEditMode = isEditMode,
      backUrl = testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )
  }

  def document(form: Form[AccountingMethod] = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm,
               isEditMode: Boolean = false): Document = {
    Jsoup.parse(page(form, isEditMode).body)
  }

  object OverseasPropertyAccountingMethodMessages {
    val heading: String = "What accounting method does your client use for their overseas property business?"
    val cash: String = "Cash basis accounting"
    val accruals: String = "Traditional accounting"
    val continue: String = "Continue"
    val saveAndContinue: String = "Save and continue"
    val update: String = "Update"
  }

  "OverseasPropertyAccountingMethod" must {
    "display the correct template details" when {
      "there is no error" in new TemplateViewTest(
        view = page(),
        title = OverseasPropertyAccountingMethodMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl)
      )
      "there is an error" in new TemplateViewTest(
        view = page(
          form = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm
            .withError(testError)
        ),
        title = OverseasPropertyAccountingMethodMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testError)
      )
    }

    "have a heading" in {
      document().mainContent.getElementsByClass("govuk-fieldset__legend").text mustBe OverseasPropertyAccountingMethodMessages.heading
    }

    "have a form" which {
      "has the correct details" in {
        val form: Element = document().mainContent.getForm
        form.attr("action") mustBe testCall.url
        form.attr("method") mustBe testCall.method
      }

      "has a set of radio buttons inputs" in {
        document().mainContent.mustHaveRadioInput(
          name = AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty,
          radioItems = Seq(
            RadioItem(
              content = Text(OverseasPropertyAccountingMethodMessages.cash),
              value = Some(AccountingMethodMapping.option_cash),
              id = Some(AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty)
            ),
            RadioItem(
              content = Text(OverseasPropertyAccountingMethodMessages.accruals),
              value = Some(AccountingMethodMapping.option_accruals),
              id = Some(s"${AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty}-2")
            )
          )
        )
      }
    }
  }

  "OverseasPropertyAccountingMethod" when {
    "not in edit mode" should {
      "have a continue button" in {
        document().mainContent.selectHead("button").text mustBe OverseasPropertyAccountingMethodMessages.saveAndContinue
      }
    }
    "in edit mode" should {
      "have an update button" in {
        document(isEditMode = true).mainContent.selectHead("button").text mustBe OverseasPropertyAccountingMethodMessages.saveAndContinue
      }
    }

  }

}
