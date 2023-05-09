
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

    val title: String = "Do your client’s business accounting periods all run from 6 April to 5 April?"
    val heading: String = "Do your client’s business accounting periods all run from 6 April to 5 April?"
    val hint: String = "The tax year runs from 6 April to 5 April. The accounting period for your client’s self-employment or property needs to be the same if you would like to sign them up to this service."
    val invalidError: String = "Select yes if all of your client’s business accounting periods are from 6 April to 5 April"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showAccountingPeriodCheck
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageMainContent: Element = doc.mainContent
  }

  "GET /client/eligibility/accounting-period-check" should {

    "return OK" in new GetSetup {
      response must have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      doc.title mustBe AccountingPeriodCheckMessages.title + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.getGovukBackLink
      backLink.attr("href") mustBe controllers.agent.eligibility.routes.PropertyTradingStartAfterController.show().url
      backLink.text mustBe AccountingPeriodCheckMessages.back
    }

    "have a view with the correct heading" in new GetSetup {
      pageMainContent.getH1Element.text mustBe AccountingPeriodCheckMessages.heading
    }

    "has a hint paragraph to explain what is accounting period check" in new GetSetup {
      pageMainContent.selectNth("p", 1).text mustBe AccountingPeriodCheckMessages.hint
    }

    "have a form" which {
      "has the correct attributes" in new GetSetup {
        val form: Element = pageMainContent.getForm
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.agent.eligibility.routes.AccountingPeriodCheckController.submit.url
      }

      "has a fieldset containing a yes and no radiobutton" in new GetSetup {
        val fieldset: Element = pageMainContent.getForm.getFieldset

        fieldset.attr("class") mustBe "govuk-fieldset"

        fieldset.selectFirst("legend").text mustBe AccountingPeriodCheckMessages.heading

        val firstRadioWithLabel: Element = fieldset.selectFirst(".govuk-radios__item:nth-of-type(1)")
        firstRadioWithLabel mustNot be (null)
        val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
        val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

        val secondRadioWithLabel: Element = fieldset.selectFirst(".govuk-radios__item:nth-of-type(2)")
        secondRadioWithLabel mustNot be (null)
        val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
        val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

        firstRadioLabel.attr("for") mustBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioLabel.text mustBe AccountingPeriodCheckMessages.yes
        firstRadioButton.attr("id") mustBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioButton.attr("name") mustBe AccountingPeriodCheckForm.accountingPeriodCheck
        firstRadioButton.attr("value") mustBe "Yes"

        secondRadioLabel.attr("for") mustBe AccountingPeriodCheckForm.accountingPeriodCheck + "-2"
        secondRadioLabel.text mustBe AccountingPeriodCheckMessages.no
        secondRadioButton.attr("id") mustBe AccountingPeriodCheckForm.accountingPeriodCheck + "-2"
        secondRadioButton.attr("name") mustBe AccountingPeriodCheckForm.accountingPeriodCheck
        secondRadioButton.attr("value") mustBe "No"
      }

      "has a button to submit" in new GetSetup {
        val submitButton: Element = pageMainContent.getForm.getGovUkSubmitButton
        submitButton.text mustBe AccountingPeriodCheckMessages.continue
        submitButton.attr("class") mustBe "govuk-button"
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
      response must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
      )
    }

    "return SEE_OTHER when selecting No and an audit has been sent" in new PostSetup(Some(No)) {
      verifyAudit()
      response must have(
        httpStatus(SEE_OTHER),
        redirectURI(routes.CannotTakePartController.show.url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response must have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).mainContent

      pageContent.select(".govuk-error-message").text mustBe s"Error: ${AccountingPeriodCheckMessages.invalidError}"
      pageContent.firstOf(s"a[href=#${AccountingPeriodCheckForm.accountingPeriodCheck}]").text mustBe AccountingPeriodCheckMessages.invalidError

      val form: Element = pageContent.getForm
      form.attr("method") mustBe "POST"
      form.attr("action") mustBe controllers.agent.eligibility.routes.AccountingPeriodCheckController.submit.url
    }

  }

}
