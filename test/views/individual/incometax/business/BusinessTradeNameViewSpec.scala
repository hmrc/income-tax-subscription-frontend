/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.individual.business.BusinessTradeNameForm
import models.individual.business.BusinessTradeNameModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.incometax.business.business_trade_name

class BusinessTradeNameViewSpec extends ViewSpec {

  object BusinessTradeNameMessages {
    val title = "What is the trade of your business?"
    val heading: String = title
    val hintText = "For example: plumbing, electrical work, consulting"
    val continue = "Continue"
    val update = "Update"
    val backLink = "Back"
  }

  val backUrl: String = testBackUrl
  val action: Call = testCall
  val taxYearEnd: Int = 2020
  val testError: FormError = FormError("businessTradeName", "testError")
  val id: String = "testId"

  class Setup(isEditMode: Boolean = false,
              businessTradeNameForm: Form[BusinessTradeNameModel] = BusinessTradeNameForm.businessTradeNameValidationForm(Nil)) {
    val page: HtmlFormat.Appendable = business_trade_name(
      businessTradeNameForm,
      testCall,
      isEditMode = isEditMode,
      testBackUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }


  "Business Trade Name" must {
    "have a title" in new Setup {
      document.title mustBe BusinessTradeNameMessages.title
    }
    "have a heading" in new Setup {
      document.getH1Element.text mustBe BusinessTradeNameMessages.heading
    }
    "have a Form" in new Setup {
      document.getForm.attr("method") mustBe testCall.method
      document.getForm.attr("action") mustBe testCall.url
    }
    "have a textInputs" in new Setup {
      document.mustHaveTextField("businessTradeName", BusinessTradeNameMessages.title)
    }
    "have a hint text for textInput" in new Setup {
      document.mustHavePara(BusinessTradeNameMessages.hintText)
    }
    "have a continue button when not in edit mode" in new Setup {
      document.getSubmitButton.text mustBe BusinessTradeNameMessages.continue
    }
    "have update button when in edit mode" in new Setup(true) {
      document.getSubmitButton.text mustBe BusinessTradeNameMessages.update
    }
    "have a backlink " in new Setup {
      document.getBackLink.text mustBe BusinessTradeNameMessages.backLink
      document.getBackLink.attr("href") mustBe testBackUrl
    }
    "must display form error on page along with textInput and hintText" in
      new Setup(false, BusinessTradeNameForm.businessTradeNameValidationForm(Nil).withError(testError)) {
        document.mustHaveErrorSummary(List[String](testError.message))
        document.mustHaveTextField("businessTradeName", BusinessTradeNameMessages.title)
        document.mustHavePara(BusinessTradeNameMessages.hintText)
      }

  }

}
