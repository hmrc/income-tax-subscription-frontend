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

package views.individual.tasklist.ukproperty

import forms.individual.business.AccountingMethodPropertyForm
import messagelookup.individual.MessageLookup.{Base => common, PropertyAccountingMethod => messages}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.individual.tasklist.ukproperty.PropertyAccountingMethod

class UkPropertyAccountingMethodViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall

  implicit val request: Request[_] = FakeRequest()
  val propertyAccountingMethod: PropertyAccountingMethod = app.injector.instanceOf[PropertyAccountingMethod]

  class Setup(isEditMode: Boolean = false) {
    val page: HtmlFormat.Appendable = propertyAccountingMethod(
      accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm,
      postAction = action,
      isEditMode,
      backUrl = backUrl
    )(FakeRequest(), implicitly)

    val document: Document = Jsoup.parse(page.body)
  }

  "property accounting method" must {

    "have a title" in new Setup {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
    }

    "have a heading and a caption" in new Setup {
      document.select(".hmrc-caption").text mustBe s"${messages.captionHidden} ${messages.captionVisible}"
      document.select("h1").text mustBe messages.heading
    }

    "have a back button" in new Setup {
      val backButton: Elements = document.select(".govuk-back-link")
      backButton.attr("href") mustBe backUrl
      backButton.text mustBe common.back
    }

    "have a accordion" which {
      "has a summary" in new Setup {
        document.select("details summary").text mustBe messages.accordionSummary
      }
      "has content" in new Setup {
        document.select("details div p:nth-of-type(1)").text mustBe messages.accordionSubHeading
        document.select("details div p:nth-of-type(2)").text mustBe messages.accordionContentPara
        document.select("details ul li").text mustBe Seq(
          messages.accordionContentBullet1,
          messages.accordionContentBullet2
        ).mkString(" ")
      }
    }

    "have a form" which {
      "has correct attributes" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe action.method
        form.attr("action") mustBe action.url
      }

      "has a cash radio button and hint" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label").text mustBe messages.radioCash
        document.select("#accountingMethodProperty-item-hint").text mustBe messages.radioCashDetail
      }

      "has a accruals radio button and hint" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label").text mustBe messages.radioAccruals
        document.select("#accountingMethodProperty-2-item-hint").text mustBe messages.radioAccrualsDetail
      }

      "has a save and continue button" that {
        s"displays ${common.saveAndContinue} when the save and retrieve feature switch is enabled" in new Setup() {
          document.select("#main-content > div > div > form > div.govuk-button-group > button:nth-child(1)").text mustBe common.saveAndContinue
        }
      }

      "has a save and come back later button and with a link that redirect to save and retrieve page" that {
        s"displays ${common.saveAndComeBackLater} save and retrieve feature switch is enabled" in new Setup() {
          val saveAndComeBackButton: Elements = document.select("#main-content > div > div > form > div.govuk-button-group > a")
          saveAndComeBackButton.text mustBe common.saveAndComeBackLater
          saveAndComeBackButton.attr("href") mustBe
            controllers.individual.tasklist.routes.ProgressSavedController.show().url + "?location=uk-property-accounting-type"
        }
      }
    }
  }
}
