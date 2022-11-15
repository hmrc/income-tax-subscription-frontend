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

package controllers.agent.eligibility

import forms.agent.PropertyTradingStartDateForm
import forms.submapping.YesNoMapping
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PropertyTradingStartAfterControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showPropertyTradingStartAfter
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageContent: Element = doc.mainContent
  }

  val date: String = LocalDate.now().minusYears(1).format(DateTimeFormatter.ofPattern("d MMMM y"))

  object PropertyStartAfterMessage {

    def title(date: String) = s"Did your client start a property business on or after $date?"

    val hint = "This includes being a landlord or using a letting agency for:"
    val point1 = "UK properties"
    val point2 = "overseas properties"
    val point3 = "holiday properties"

    def error(date: String) = s"Select yes if your client owns a property business that began trading on or after $date"

    val back: String = "Back"
    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /eligibility/property-start-date" should {

    "return OK" in new GetSetup {
      response must have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {

      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      doc.title mustBe s"${PropertyStartAfterMessage.title(date) + serviceNameGovUk}"
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text mustBe PropertyStartAfterMessage.title(date)
    }

    "have a view with the correct hint" in new GetSetup {
      pageContent.firstOf("p").text mustBe PropertyStartAfterMessage.hint
      pageContent.getNthUnorderedList(1).getNthListItem(1).text mustBe PropertyStartAfterMessage.point1
      pageContent.getNthUnorderedList(1).getNthListItem(2).text mustBe PropertyStartAfterMessage.point2
      pageContent.getNthUnorderedList(1).getNthListItem(3).text mustBe PropertyStartAfterMessage.point3
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.getGovukBackLink
      backLink.attr("href") mustBe controllers.agent.eligibility.routes.SoleTraderController.show().url
      backLink.text mustBe PropertyStartAfterMessage.back
    }

    "have a form with the correct inputs and values" in new GetSetup {
      val form: Element = doc.firstOf("form")

      val yesRadio: Element = form.selectNth(".govuk-radios__item", 1).firstOf("input")
      val yesLabel: Element = form.selectNth(".govuk-radios__item", 1).firstOf("label")

      val noRadio: Element = form.selectNth(".govuk-radios__item", 2).firstOf("input")
      val noLabel: Element = form.selectNth(".govuk-radios__item", 2).firstOf("label")

      yesRadio.attr("type") mustBe "radio"
      yesRadio.attr("value") mustBe YesNoMapping.option_yes
      yesRadio.attr("name") mustBe PropertyTradingStartDateForm.fieldName
      yesRadio.attr("id") mustBe PropertyTradingStartDateForm.fieldName

      yesLabel.text mustBe PropertyStartAfterMessage.yes
      yesLabel.attr("for") mustBe PropertyTradingStartDateForm.fieldName

      noRadio.attr("type") mustBe "radio"
      noRadio.attr("value") mustBe YesNoMapping.option_no
      noRadio.attr("name") mustBe PropertyTradingStartDateForm.fieldName
      noRadio.attr("id") mustBe s"${PropertyTradingStartDateForm.fieldName}-2"

      noLabel.text mustBe PropertyStartAfterMessage.no
      noLabel.attr("for") mustBe s"${PropertyTradingStartDateForm.fieldName}-2"

      val submitButton: Elements = form.select("button[class=govuk-button]")
      submitButton.text mustBe PropertyStartAfterMessage.continue
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") mustBe "POST"
      form.attr("action") mustBe controllers.agent.eligibility.routes.PropertyTradingStartAfterController.submit().url
    }
  }

  class PostSetup(answer: Option[YesNo]) {
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitPropertyTradingStartAfter(answer)
  }

  "POST /eligibility/property-start-date" should {

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
        redirectURI(controllers.agent.eligibility.routes.AccountingPeriodCheckController.show.url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response must have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).mainContent

      pageContent.firstOf("p[class=govuk-error-message]").text mustBe s"Error: ${PropertyStartAfterMessage.error(date)}"
      pageContent.firstOf(s"a[href=#${PropertyTradingStartDateForm.fieldName}]").text mustBe PropertyStartAfterMessage.error(date)
    }

  }

}
