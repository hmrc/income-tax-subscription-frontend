
package controllers.agent.eligibility

import forms.agent.AccountingPeriodCheckForm
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AccountingPeriodCheckControllerISpec extends ComponentSpecBase {

  object AccountingPeriodCheckMessages {
    val back: String = "Back"

    val title: String = "Are all of your client’s business accounting periods from 6 April to 5 April?"
    val heading: String = "Are all of your client’s business accounting periods from 6 April to 5 April?"
    val invalidError: String = "Select yes if all of your client’s business accounting periods are from 6 April to 5 April"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showAccountingPeriodCheck
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageContent: Element = doc.content
  }

  "GET /client/eligibility/accounting-period-check" should {

    "return OK" in new GetSetup {
      response should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      doc.title shouldBe AccountingPeriodCheckMessages.title + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = pageContent.getBackLink
      backLink.attr("href") shouldBe controllers.agent.eligibility.routes.PropertyTradingStartAfterController.show().url
      backLink.text shouldBe AccountingPeriodCheckMessages.back
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text shouldBe AccountingPeriodCheckMessages.heading
    }

    "have a form" which {
      "has the correct attributes" in new GetSetup {
        val form: Element = pageContent.getForm
        form.attr("method") shouldBe "POST"
        form.attr("action") shouldBe controllers.agent.eligibility.routes.AccountingPeriodCheckController.submit().url
      }

      "has a fieldset containing a yes and no radiobutton" in new GetSetup {
        val fieldset: Element = pageContent.getForm.getFieldset

        fieldset.attr("class") shouldBe "inline"

        fieldset.selectFirst("legend").text shouldBe AccountingPeriodCheckMessages.heading

        val firstRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(1)")
        val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
        val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

        val secondRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(2)")
        val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
        val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

        firstRadioLabel.attr("for") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioLabel.text shouldBe AccountingPeriodCheckMessages.yes
        firstRadioButton.attr("id") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioButton.attr("name") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioButton.attr("value") shouldBe "Yes"

        secondRadioLabel.attr("for") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck + "-2"
        secondRadioLabel.text shouldBe AccountingPeriodCheckMessages.no
        secondRadioButton.attr("id") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck + "-2"
        secondRadioButton.attr("name") shouldBe AccountingPeriodCheckForm.accountingPeriodCheck
        secondRadioButton.attr("value") shouldBe "No"
      }

      "has a button to submit" in new GetSetup {
        val submitButton: Element = pageContent.getForm.getSubmitButton
        submitButton.text shouldBe AccountingPeriodCheckMessages.continue
        submitButton.attr("class") shouldBe "button"
        submitButton.attr("type") shouldBe "submit"
      }
    }
  }

  class PostSetup(answer: Option[YesNo]) {
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitAccountingPeriodCheck(answer)
  }

  "POST /client/eligibility/accounting-period-check" should {

    "return SEE_OTHER when selecting yes and an audit has been sent" in new PostSetup(Some(Yes)) {
      verifyAudit()
      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
      )
    }

    "return SEE_OTHER when selecting No and an audit has been sent" in new PostSetup(Some(No)) {
      verifyAudit()
      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(routes.CannotTakePartController.show().url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response should have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).content

      pageContent.select("span[class=error-notification bold]").text shouldBe AccountingPeriodCheckMessages.invalidError
      pageContent.select(s"a[href=#${AccountingPeriodCheckForm.accountingPeriodCheck}]").text shouldBe AccountingPeriodCheckMessages.invalidError
    }

  }

}
