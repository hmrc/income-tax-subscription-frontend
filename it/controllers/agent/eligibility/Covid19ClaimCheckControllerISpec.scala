
package controllers.agent.eligibility

import forms.agent.Covid19ClaimCheckForm
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.{No, Yes, YesNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class Covid19ClaimCheckControllerISpec extends ComponentSpecBase {

  trait GetSetup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showCovid19ClaimCheck()
    val doc: Document = Jsoup.parse(result.body)
    val pageContent: Element = doc.content
  }

  object Covid19ClaimCheckMessages {
    val title: String = "Has your client ever claimed a coronavirus (COVID‑19) grant or intend to do so in the future?"
    val heading: String = "Has your client ever claimed a coronavirus (COVID‑19) grant or intend to do so in the future?"
    val join_pilot: String = "You are unable to sign up your client to this pilot if they:"
    val join_pilot_point_1: String = "have claimed through the Self-Employment Support Scheme (opens in new tab) for sole traders"
    val join_pilot_point_2: String = "are an employer and have furloughed one or more of their employees using the Coronavirus Job Retention Scheme (opens in new tab)"
    val claim_sick_pay: String = "If your client has claimed a rebate through the Coronavirus Statutory Sick Pay Rebate Scheme (opens in new tab) and has not or will not make a claim for the Self Employment Support Scheme or Coronavirus Job Retention Scheme, you can still sign them up for the pilot."
    val error: String = "Select yes if your client has ever claimed or intends to claim a coronavirus (COVID‑19) grant"

    val yes: String = "Yes"
    val no: String = "No"
    val continue: String = "Continue"
  }

  "GET /client/eligibility/covid-19" should {

    "return OK" in new GetSetup {
      result should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new GetSetup {
      doc.title shouldBe Covid19ClaimCheckMessages.title
    }

    "have a view with the correct heading" in new GetSetup {
      pageContent.getH1Element.text shouldBe Covid19ClaimCheckMessages.heading
    }

    "have a paragaph stating how to join the pilot" in new GetSetup {
      pageContent.getNthParagraph(1).text shouldBe Covid19ClaimCheckMessages.join_pilot
    }

    "have a bullet point list of reasons unable to join the pilot" in new GetSetup {
      pageContent.getNthUnorderedList(1).getNthListItem(1).text shouldBe Covid19ClaimCheckMessages.join_pilot_point_1
      pageContent.getNthUnorderedList(1).getNthListItem(2).text shouldBe Covid19ClaimCheckMessages.join_pilot_point_2
    }

    "have a paragraph stating about Coronavirus SSP Scheme" in new GetSetup {
      pageContent.getNthParagraph(2).text shouldBe Covid19ClaimCheckMessages.claim_sick_pay
    }

    "have a form" in new GetSetup {
      val form: Element = pageContent.getForm
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe controllers.agent.eligibility.routes.Covid19ClaimCheckController.submit().url
    }

    "have a button to submit" in new GetSetup {
      val submitButton: Element = pageContent.getForm.getSubmitButton
      submitButton.text shouldBe Covid19ClaimCheckMessages.continue
      submitButton.attr("class") shouldBe "button"
      submitButton.attr("type") shouldBe "submit"
    }

    "have a fieldset containing a yes and no radiobutton" in new GetSetup {
      val fieldset: Element = pageContent.getFieldset

      fieldset.attr("class") shouldBe "inline"

      fieldset.selectFirst("legend").text shouldBe Covid19ClaimCheckMessages.heading

      val firstRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(1)")
      val firstRadioLabel: Element = firstRadioWithLabel.selectFirst("label")
      val firstRadioButton: Element = firstRadioWithLabel.selectFirst("input")

      val secondRadioWithLabel: Element = fieldset.selectFirst(".multiple-choice:nth-of-type(2)")
      val secondRadioLabel: Element = secondRadioWithLabel.selectFirst("label")
      val secondRadioButton: Element = secondRadioWithLabel.selectFirst("input")

      firstRadioLabel.attr("for") shouldBe Covid19ClaimCheckForm.fieldName
      firstRadioButton.attr("id") shouldBe Covid19ClaimCheckForm.fieldName
      firstRadioButton.attr("name") shouldBe Covid19ClaimCheckForm.fieldName
      firstRadioButton.attr("value") shouldBe Covid19ClaimCheckMessages.yes

      secondRadioLabel.attr("for") shouldBe Covid19ClaimCheckForm.fieldName + "-2"
      secondRadioButton.attr("id") shouldBe Covid19ClaimCheckForm.fieldName + "-2"
      secondRadioButton.attr("name") shouldBe Covid19ClaimCheckForm.fieldName
      secondRadioButton.attr("value") shouldBe Covid19ClaimCheckMessages.no
    }

  }

  class PostSetup(answer: Option[YesNo]){
    AuthStub.stubAuthSuccess()

    val response: WSResponse = IncomeTaxSubscriptionFrontend.submitCovid19ClaimCheck(answer)
  }

  "POST /eligibility/covid-19" should {
    "return SEE_OTHER when selecting Yes" in new PostSetup(Some(Yes)) {
      response should have (
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.eligibility.routes.CovidCannotSignUpController.show().url)
      )
    }

    "return SEE_OTHER when selecting No" in new PostSetup(Some(No)) {
      response should have (
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.show().url)
      )
    }

    "return a BAD_REQUEST when no answer is selected" in new PostSetup(None) {
      response should have (
        httpStatus(BAD_REQUEST)
      )

      val pageContent: Element = Jsoup.parse(response.body).content

      pageContent.select("span[class=error-notification]").text shouldBe Covid19ClaimCheckMessages.error
      pageContent.select(s"a[href=#${Covid19ClaimCheckForm.fieldName}]").text shouldBe Covid19ClaimCheckMessages.error
    }
  }

}
