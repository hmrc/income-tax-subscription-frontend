
package controllers.agent.eligibility

import forms.agent.OtherSourcesOfIncomeForm
import helpers.ViewSpec
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class OtherSourcesOfIncomeControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    lazy val response: WSResponse = IncomeTaxSubscriptionFrontend.showOtherSourcesOfIncome
    lazy val doc: Document = Jsoup.parse(response.body)
    lazy val pageContent: Element = doc.content
  }

  object OtherSourcesOfIncomeMessages {
    val back: String = "Back"

    val title: String = "Apart from self employment or renting out property, does your client have any other sources of income?"
    val heading: String = "Apart from self employment or renting out property, does your client have any other sources of income?"
    val include: String = "This could include:"
    val includePoint1: String = "as an employee"
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
      response should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      doc.title shouldBe OtherSourcesOfIncomeMessages.title
    }

    "have a view with a back link" in new GetSetup {
      val backLink: Element = pageContent.getBackLink
      backLink.attr("href") shouldBe controllers.agent.eligibility.routes.Covid19ClaimCheckController.show().url
      backLink.text shouldBe OtherSourcesOfIncomeMessages.back
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text shouldBe OtherSourcesOfIncomeMessages.heading
    }

    "have a paragraph stating what is included" in new GetSetup {
      pageContent.getNthParagraph(1).text shouldBe OtherSourcesOfIncomeMessages.include
    }

    "have a bullet list of included incomes" in new GetSetup {
      pageContent.getNthUnorderedList(1).getNthListItem(1).text shouldBe OtherSourcesOfIncomeMessages.includePoint1
      pageContent.getNthUnorderedList(1).getNthListItem(2).text shouldBe OtherSourcesOfIncomeMessages.includePoint2
      pageContent.getNthUnorderedList(1).getNthListItem(3).text shouldBe OtherSourcesOfIncomeMessages.includePoint3
      pageContent.getNthUnorderedList(1).getNthListItem(4).text shouldBe OtherSourcesOfIncomeMessages.includePoint4
      pageContent.getNthUnorderedList(1).getNthListItem(5).text shouldBe OtherSourcesOfIncomeMessages.includePoint5
    }

    "have a paragraph stating what is not included" in new GetSetup {
      pageContent.getNthParagraph(2).text shouldBe OtherSourcesOfIncomeMessages.notInclude
    }

    "have a bullet list of not included incomes" in new GetSetup {
      pageContent.getNthUnorderedList(2).getNthListItem(1).text shouldBe OtherSourcesOfIncomeMessages.notIncludePoint1
      pageContent.getNthUnorderedList(2).getNthListItem(2).text shouldBe OtherSourcesOfIncomeMessages.notIncludePoint2
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.submit().url
    }

    "have a button to submit" in new GetSetup {
      val submitButton: Element = pageContent.getForm.getSubmitButton
      submitButton.text shouldBe OtherSourcesOfIncomeMessages.continue
      submitButton.attr("class") shouldBe "button"
      submitButton.attr("type") shouldBe "submit"
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {
      val fieldset: Element = pageContent.getFieldset

      fieldset.attr("class") shouldBe "inline"

      fieldset.selectFirst("legend").text shouldBe OtherSourcesOfIncomeMessages.heading

      val firstRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(1)")
      val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
      val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

      val secondRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(2)")
      val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
      val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

      firstRadioLabel.attr("for") shouldBe OtherSourcesOfIncomeForm.fieldName
      firstRadioButton.attr("id") shouldBe OtherSourcesOfIncomeForm.fieldName
      firstRadioButton.attr("name") shouldBe OtherSourcesOfIncomeForm.fieldName
      firstRadioButton.attr("value") shouldBe "Yes"

      secondRadioLabel.attr("for") shouldBe OtherSourcesOfIncomeForm.fieldName + "-2"
      secondRadioButton.attr("id") shouldBe OtherSourcesOfIncomeForm.fieldName + "-2"
      secondRadioButton.attr("name") shouldBe OtherSourcesOfIncomeForm.fieldName
      secondRadioButton.attr("value") shouldBe "No"
    }
  }

  class PostSetup(answer: Option[YesNo]) {
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitOtherSourcesOfIncome(answer)
  }

  "POST /eligibility/other-income" should {

    "return SEE_OTHER when selecting yes" in new PostSetup(Some(Yes)) {
      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show().url)
      )
    }

    "return SEE_OTHER when selecting No" in new PostSetup(Some(No)) {

      response should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.matching.routes.ClientDetailsController.show().url)
      )
    }

    "return BADREQUEST when no Answer is given" in new PostSetup(None) {
      response should have(
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).content

      pageContent.select("span[class=error-notification]").text shouldBe OtherSourcesOfIncomeMessages.invalidError
      pageContent.select(s"a[href=#${OtherSourcesOfIncomeForm.fieldName}]").text shouldBe OtherSourcesOfIncomeMessages.invalidError
    }

  }

}