
package controllers.agent.eligibility

import forms.agent.OtherSourcesOfIncomeForm
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.servicemocks.AuditStub.verifyAudit
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class OtherSourcesOfIncomeControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showOtherSourcesOfIncome
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageContent: Element = doc.mainContent
  }

  object OtherSourcesOfIncomeMessages {
    val back: String = "Back"

    val title: String = "Apart from self employment or renting out property, does your client have any other sources of income?"
    val heading: String = "Apart from self employment or renting out property, does your client have any other sources of income?"
    val include: String = "This could include:"
    val includePoint1: String = "PAYE as an employee"
    val includePoint2: String = "UK pensions or annuities"
    val includePoint3: String = "investments from outside the UK"
    val includePoint4: String = "capital gains"
    val includePoint5: String = "taxable state benefits"
    val notInclude: String = "This does not include:"
    val notIncludePoint1: String = "bank and building society interest"
    val notIncludePoint2: String = "dividends"
    val invalidError: String = "Select yes if your client has sources of income other than self employment or property income"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /eligibility/client/other-income" should {

    "return OK" in new GetSetup {
      response must have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      doc.title mustBe OtherSourcesOfIncomeMessages.title + serviceNameGovUk
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = doc.select(".govuk-back-link").first()
      backLink.attr("href") mustBe appConfig.incomeTaxEligibilityFrontendUrl + "/client/terms-of-participation"
      backLink.text mustBe OtherSourcesOfIncomeMessages.back
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text mustBe OtherSourcesOfIncomeMessages.heading
    }

    "have a paragraph stating what is included" in new GetSetup {
      pageContent.getNthParagraph(1).text mustBe OtherSourcesOfIncomeMessages.include
    }

    "have a bullet list of included incomes" in new GetSetup {
      pageContent.getNthUnorderedList(1).getNthListItem(1).text mustBe OtherSourcesOfIncomeMessages.includePoint1
      pageContent.getNthUnorderedList(1).getNthListItem(2).text mustBe OtherSourcesOfIncomeMessages.includePoint2
      pageContent.getNthUnorderedList(1).getNthListItem(3).text mustBe OtherSourcesOfIncomeMessages.includePoint3
      pageContent.getNthUnorderedList(1).getNthListItem(4).text mustBe OtherSourcesOfIncomeMessages.includePoint4
      pageContent.getNthUnorderedList(1).getNthListItem(5).text mustBe OtherSourcesOfIncomeMessages.includePoint5
    }

    "have a paragraph stating what is not included" in new GetSetup {
      pageContent.getNthParagraph(2).text mustBe OtherSourcesOfIncomeMessages.notInclude
    }

    "have a bullet list of not included incomes" in new GetSetup {
      pageContent.getNthUnorderedList(2).getNthListItem(1).text mustBe OtherSourcesOfIncomeMessages.notIncludePoint1
      pageContent.getNthUnorderedList(2).getNthListItem(2).text mustBe OtherSourcesOfIncomeMessages.notIncludePoint2
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") mustBe "POST"
      form.attr("action") mustBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.submit.url
    }

    "have a button to submit" in new GetSetup {
      val submitButton: Element = pageContent.getForm.getGovUkSubmitButton
      submitButton.text mustBe OtherSourcesOfIncomeMessages.continue
      submitButton.attr("class") mustBe "govuk-button"
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {
      val fieldset: Element = pageContent.getFieldset

      fieldset.attr("class") mustBe "govuk-fieldset"

      fieldset.selectFirst("legend").text mustBe OtherSourcesOfIncomeMessages.heading

      val firstRadioWithLabel: Element = fieldset.selectFirst(".govuk-radios__item:nth-of-type(1)")
      val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
      val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

      val secondRadioWithLabel: Element = fieldset.selectFirst(".govuk-radios__item:nth-of-type(2)")
      val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
      val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

      firstRadioLabel.attr("for") mustBe OtherSourcesOfIncomeForm.fieldName
      firstRadioLabel.text mustBe OtherSourcesOfIncomeMessages.yes
      firstRadioButton.attr("id") mustBe OtherSourcesOfIncomeForm.fieldName
      firstRadioButton.attr("name") mustBe OtherSourcesOfIncomeForm.fieldName
      firstRadioButton.attr("value") mustBe "Yes"

      secondRadioLabel.attr("for") mustBe OtherSourcesOfIncomeForm.fieldName + "-2"
      secondRadioLabel.text mustBe OtherSourcesOfIncomeMessages.no
      secondRadioButton.attr("id") mustBe OtherSourcesOfIncomeForm.fieldName + "-2"
      secondRadioButton.attr("name") mustBe OtherSourcesOfIncomeForm.fieldName
      secondRadioButton.attr("value") mustBe "No"
    }
  }

  class PostSetup(answer: Option[YesNo]) {
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitOtherSourcesOfIncome(answer)
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
        redirectURI(controllers.agent.eligibility.routes.SoleTraderController.show().url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response must have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).mainContent

      pageContent.firstOf("p[class=govuk-error-message]").text mustBe s"Error: ${OtherSourcesOfIncomeMessages.invalidError}"
      pageContent.firstOf(s"a[href=#${OtherSourcesOfIncomeForm.fieldName}]").text mustBe OtherSourcesOfIncomeMessages.invalidError

      val form: Element = pageContent.getForm
      form.attr("method") mustBe "POST"
      form.attr("action") mustBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.submit.url
    }

  }

}
