
package controllers.agent.eligibility

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import forms.agent.{PropertyTradingStartDateForm, SoleTraderForm}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import forms.submapping.YesNoMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

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

    def title(date: String) = s"Did your client’s sole trader business start trading on or after $date?"

    def heading(date: String) = s"Did your client’s sole trader business start trading on or after $date?"

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
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
       doc.title shouldBe s"${SoleTraderPageMessages.heading(date)}" + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.getGovukBackLink
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

      val submitButton: Elements = pageContent.getForm.select("button[class=govuk-button]")
      submitButton.text shouldBe SoleTraderPageMessages.continue
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {

      val form: Element = doc.firstOf("form")
      val yesRadio: Element = form.selectNth(".govuk-radios__item", 1).firstOf("input")
      val yesLabel: Element = form.selectNth(".govuk-radios__item", 1).firstOf("label")

      val noRadio: Element = form.selectNth(".govuk-radios__item", 2).firstOf("input")
      val noLabel: Element = form.selectNth(".govuk-radios__item", 2).firstOf("label")

      yesRadio.attr("type") shouldBe "radio"
      yesRadio.attr("value") shouldBe YesNoMapping.option_yes
      yesRadio.attr("name") shouldBe PropertyTradingStartDateForm.fieldName
      yesRadio.attr("id") shouldBe PropertyTradingStartDateForm.fieldName

      yesLabel.text shouldBe SoleTraderPageMessages.yes
      yesLabel.attr("for") shouldBe PropertyTradingStartDateForm.fieldName

      noRadio.attr("type") shouldBe "radio"
      noRadio.attr("value") shouldBe YesNoMapping.option_no
      noRadio.attr("name") shouldBe PropertyTradingStartDateForm.fieldName
      noRadio.attr("id") shouldBe s"${PropertyTradingStartDateForm.fieldName}-2"

      noLabel.text shouldBe SoleTraderPageMessages.no
      noLabel.attr("for") shouldBe s"${PropertyTradingStartDateForm.fieldName}-2"
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

        val pageContent: Element = Jsoup.parse(response.body).mainContent

        pageContent.firstOf("span[class=govuk-error-message]").text shouldBe s"Error: ${SoleTraderPageMessages.invalidError(date)}"
        pageContent.firstOf(s"a[href=#${SoleTraderForm.fieldName}]").text shouldBe s"${SoleTraderPageMessages.invalidError(date)}"
      }

    }
  }
}
