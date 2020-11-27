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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import forms.agent.PropertyTradingStartDateForm
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER}

class PropertyTradingStartAfterControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showPropertyTradingStartAfter
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageContent: Element = doc.content
  }

  val date: String = LocalDate.now().minusYears(1).format(DateTimeFormatter.ofPattern("d MMMM y"))

  object PropertyStartAfterMessage {

    def title(date: String) = s"Does your client own a property business that began trading on or after $date?"

    val hint = "This includes being a landlord and letting holiday properties"

    def error(date: String) = s"Select yes if your client owns a property business that began trading on or after $date"

    val back: String = "Back"
    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /eligibility/property-start-date " should {

    "return OK" in new GetSetup {
      response should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {

      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      doc.title shouldBe s"${PropertyStartAfterMessage.title(date) + serviceNameGovUk}"
    }

    "have a view with the correct heading" in new GetSetup {
      doc.getH1Element.text shouldBe PropertyStartAfterMessage.title(date)
    }

    "have a view with the correct hint" in new GetSetup {
      doc.getHintText shouldBe PropertyStartAfterMessage.hint
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.getBackLink
      backLink.attr("href") shouldBe controllers.agent.eligibility.routes.SoleTraderController.show().url
      backLink.text shouldBe PropertyStartAfterMessage.back
    }

    "have a view with the correct values displayed in the form" in new GetSetup {
      val form = doc.select("form")
      val labels = doc.select("form").select("label")
      val radios = form.select("input[type=radio]")

      radios.size() shouldBe 2
      radios.get(0).attr("id") shouldBe "yes-no"
      labels.get(0).text() shouldBe PropertyStartAfterMessage.yes

      radios.get(1).attr("id") shouldBe "yes-no-2"
      labels.get(1).text() shouldBe PropertyStartAfterMessage.no

      val submitButton = form.select("button[type=submit]")
      submitButton.text shouldBe PropertyStartAfterMessage.continue
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe controllers.agent.eligibility.routes.PropertyTradingStartAfterController.submit().url
    }
  }

  class PostSetup(answer: Option[YesNo]) {
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitPropertyTradingStartAfter(answer)
  }

  "POST /eligibility/property-start-date" should {

    "return SEE_OTHER when selecting yes and an audit has been sent" in new PostSetup(Some(Yes)) {
      verifyAudit()
      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show().url)
      )
    }

    "return SEE_OTHER when selecting No and an audit has been sent" in new PostSetup(Some(No)) {
      verifyAudit()
      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.eligibility.routes.AccountingPeriodCheckController.show().url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response should have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).content

      pageContent.select("span[class=error-notification bold]").text shouldBe PropertyStartAfterMessage.error(date)
      pageContent.select(s"a[href=#${PropertyTradingStartDateForm.fieldName}]").text shouldBe PropertyStartAfterMessage.error(date)
    }

  }

}
