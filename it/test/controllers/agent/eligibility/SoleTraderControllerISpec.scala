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

package controllers.agent.eligibility

import forms.agent.{PropertyTradingStartDateForm, SoleTraderForm}
import forms.submapping.YesNoMapping
import helpers.IntegrationTestConstants.{testFormattedNino, testFullName}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SoleTraderControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showSoleTrader()
    val doc: Document = Jsoup.parse(result.body)
    lazy val pageContent: Element = doc.mainContent
  }

  val date: String = LocalDate.now().minusYears(2).format(DateTimeFormatter.ofPattern("d MMMM y"))

  object SoleTraderPageMessages {
    val back: String = "Back"

    def heading(date: String) = s"Did your client’s business start trading on or after $date?"

    val caption: String = s"$testFullName | $testFormattedNino"

    def invalidError(date: String) = s"Select yes if your client is a sole trader that began trading on or after $date?"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /eligibility/sole-trader-start-date" should {
    "return OK" in new GetSetup {
      result must have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      doc.title mustBe s"${SoleTraderPageMessages.heading(date)}" + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.getGovukBackLink
      backLink.attr("href") mustBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.show.url
      backLink.text mustBe SoleTraderPageMessages.back
    }

    "have a view with the correct heading and caption" in new GetSetup {
      val header: Element = pageContent
      header.selectHead(".govuk-heading-l").text mustBe SoleTraderPageMessages.heading(date)
      header.selectHead(".govuk-caption-l").text mustBe SoleTraderPageMessages.caption
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") mustBe "POST"
      form.attr("action") mustBe controllers.agent.eligibility.routes.SoleTraderController.submit().url
    }

    "have a button to submit" in new GetSetup {

      val submitButton: Elements = pageContent.getForm.select("button[class=govuk-button]")
      submitButton.text mustBe SoleTraderPageMessages.continue
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {

      val form: Element = doc.selectHead("form")
      val yesRadio: Element = form.selectNth(".govuk-radios__item", 1).selectHead("input")
      val yesLabel: Element = form.selectNth(".govuk-radios__item", 1).selectHead("label")

      val noRadio: Element = form.selectNth(".govuk-radios__item", 2).selectHead("input")
      val noLabel: Element = form.selectNth(".govuk-radios__item", 2).selectHead("label")

      yesRadio.attr("type") mustBe "radio"
      yesRadio.attr("value") mustBe YesNoMapping.option_yes
      yesRadio.attr("name") mustBe PropertyTradingStartDateForm.fieldName
      yesRadio.attr("id") mustBe PropertyTradingStartDateForm.fieldName

      yesLabel.text mustBe SoleTraderPageMessages.yes
      yesLabel.attr("for") mustBe PropertyTradingStartDateForm.fieldName

      noRadio.attr("type") mustBe "radio"
      noRadio.attr("value") mustBe YesNoMapping.option_no
      noRadio.attr("name") mustBe PropertyTradingStartDateForm.fieldName
      noRadio.attr("id") mustBe s"${PropertyTradingStartDateForm.fieldName}-2"

      noLabel.text mustBe SoleTraderPageMessages.no
      noLabel.attr("for") mustBe s"${PropertyTradingStartDateForm.fieldName}-2"
    }

    class PostSetup(answer: Option[YesNo]) {
      AuthStub.stubAuthSuccess()

      val response: WSResponse = IncomeTaxSubscriptionFrontend.submitSoleTraderForm(answer)
    }

    "POST /eligibility/other-income" should {

      "return SEE_OTHER when selecting yes and an audit has been sent" in new PostSetup(Some(Yes)) {
        verifyAudit()
        response must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show.url)
        )
      }

      "return SEE_OTHER when selecting No and an audit has been sent" in new PostSetup(Some(No)) {
        verifyAudit()
        response must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.PropertyTradingStartAfterController.show().url)
        )
      }

      "return BADREQUEST when no Answer is given" in new PostSetup(None) {
        response must have(
          httpStatus(BAD_REQUEST)
        )

        val pageContent: Element = Jsoup.parse(response.body).mainContent

        pageContent.selectHead("p[class=govuk-error-message]").text mustBe s"Error: ${SoleTraderPageMessages.invalidError(date)}"
        pageContent.selectHead(s"a[href=#${SoleTraderForm.fieldName}]").text mustBe s"${SoleTraderPageMessages.invalidError(date)}"
      }

    }
  }
}
