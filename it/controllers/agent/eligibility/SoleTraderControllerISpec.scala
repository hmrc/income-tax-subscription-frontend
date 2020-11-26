
package controllers.agent.eligibility

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import forms.agent.SoleTraderForm
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class SoleTraderControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showSoleTrader()
    val doc: Document = Jsoup.parse(result.body)
    val pageContent: Element = doc.content
  }

  val date: String = LocalDate.now().minusYears(2).format(DateTimeFormatter.ofPattern("d MMMM y"))

  object SoleTraderPageMessages {
    val back: String = "Back"

    def title(date: String) = s"Is your client a sole trader that began trading on or after $date?"

    def heading(date: String) = s"Is your client a sole trader that began trading on or after $date?"

    def invalidError(date: String) = s"Select yes if your client is a sole trader that began trading on or after $date?"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /eligibility/sole-trader-start-date" should {

    "return OK" in new GetSetup {
      result should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      doc.title shouldBe s"${SoleTraderPageMessages.heading(date)}" + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = pageContent.getBackLink
      backLink.attr("href") shouldBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.show().url
      backLink.text shouldBe SoleTraderPageMessages.back
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text shouldBe s"${SoleTraderPageMessages.heading(date)}"
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe controllers.agent.eligibility.routes.SoleTraderController.submit().url
    }

    "have a button to submit" in new GetSetup {
      val submitButton: Element = pageContent.getForm.getSubmitButton
      submitButton.text shouldBe SoleTraderPageMessages.continue
      submitButton.attr("class") shouldBe "button"
      submitButton.attr("type") shouldBe "submit"
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {
      val fieldset: Element = pageContent.getFieldset

      fieldset.attr("class") shouldBe "inline"

      fieldset.selectFirst("legend").text shouldBe s"${SoleTraderPageMessages.title(date)}"

      val firstRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(1)")
      val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
      val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

      val secondRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(2)")
      val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
      val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

      firstRadioLabel.attr("for") shouldBe SoleTraderForm.fieldName
      firstRadioLabel.text shouldBe SoleTraderPageMessages.yes
      firstRadioButton.attr("id") shouldBe SoleTraderForm.fieldName
      firstRadioButton.attr("name") shouldBe SoleTraderForm.fieldName
      firstRadioButton.attr("value") shouldBe "Yes"

      secondRadioLabel.attr("for") shouldBe SoleTraderForm.fieldName + "-2"
      secondRadioLabel.text shouldBe SoleTraderPageMessages.no
      secondRadioButton.attr("id") shouldBe SoleTraderForm.fieldName + "-2"
      secondRadioButton.attr("name") shouldBe SoleTraderForm.fieldName
      secondRadioButton.attr("value") shouldBe "No"
    }

    class PostSetup(answer: Option[YesNo]) {
      AuthStub.stubAuthSuccess()

      val response: WSResponse = IncomeTaxSubscriptionFrontend.submitSoleTraderForm(answer)
    }

    "POST /eligibility/other-income" should {

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
          redirectURI(controllers.agent.eligibility.routes.PropertyTradingStartAfterController.show().url)
        )
      }

      "return BADREQUEST when no Answer is given" in new PostSetup(None) {
        response should have(
          httpStatus(BAD_REQUEST)
        )

        val pageContent: Element = Jsoup.parse(response.body).content

        pageContent.select("span[class=error-notification bold]").text shouldBe s"${SoleTraderPageMessages.invalidError(date)}"
        pageContent.select(s"a[href=#${SoleTraderForm.fieldName}]").text shouldBe s"${SoleTraderPageMessages.invalidError(date)}"
      }

    }
  }
}
