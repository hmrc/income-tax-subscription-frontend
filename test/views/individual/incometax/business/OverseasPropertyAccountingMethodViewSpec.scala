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

package views.individual.incometax.business

import assets.MessageLookup.OverseasPropertyAccountingMethod.{radioAccruals, radioAccrualsDetail, radioCash, radioCashDetail}
import assets.MessageLookup.{Base => common, OverseasPropertyAccountingMethod => messages}
import forms.individual.business.AccountingMethodOverseasPropertyForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.incometax.business.OverseasPropertyAccountingMethod

class OverseasPropertyAccountingMethodViewSpec extends ViewSpec {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall

  val overseasPropertyAccountingMethod: OverseasPropertyAccountingMethod = app.injector.instanceOf[OverseasPropertyAccountingMethod]

  class Setup(isEditMode: Boolean = false) {
    val page: HtmlFormat.Appendable = overseasPropertyAccountingMethod(
      overseasPropertyAccountingMethodForm = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm,
      postAction = action,
      isEditMode,
      backUrl = backUrl
    )(FakeRequest(), implicitly)

    val document: Document = Jsoup.parse(page.body)
  }

  "overseas property accounting method" must {

    "have a title" in new Setup {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
    }

    "have a heading" in new Setup {
      document.select("h1").text mustBe messages.heading
    }

    "have a caption" in new Setup {
      document.selectHead(".hmrc-caption").text mustBe s"${messages.captionHidden} ${messages.captionVisible}"
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
        document.select("details div p").text mustBe messages.accordionContentPara
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
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label").text mustBe radioCash
        document.select("#accountingMethodOverseasProperty-item-hint").text mustBe radioCashDetail
      }

      "has a accruals radio button" in new Setup {
        document.select("#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label").text mustBe radioAccruals
        document.select("#accountingMethodOverseasProperty-2-item-hint").text mustBe radioAccrualsDetail
      }


      "has a save and continue button" that {
        s"displays ${common.saveAndContinue} when save and retrieve feature switch is enabled" in new Setup() {
          document.mainContent.selectHead("div.govuk-button-group").selectHead("button").text mustBe common.saveAndContinue
        }
      }

      "has a save and come back later button" that {
        s"displays ${common.saveAndComeBackLater} when save and retrieve feature switch is enabled" in new Setup() {
          val saveAndComeBackButton: Element = document.mainContent.selectHead("div.govuk-button-group").selectHead("a")
          saveAndComeBackButton.text mustBe common.saveAndComeBackLater
          saveAndComeBackButton.attr("href") mustBe
            controllers.individual.business.routes.ProgressSavedController.show().url + "?location=overseas-property-accounting-type"
        }
      }
    }
  }
}
