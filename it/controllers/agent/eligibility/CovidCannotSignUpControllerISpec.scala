
package controllers.agent.eligibility

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._


class CovidCannotSignUpControllerISpec extends ComponentSpecBase{


  trait Setup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showCovidCannotSignUp()
    val doc: Document = Jsoup.parse(result.body)
    val pageContent: Element = doc.content
  }

  object CovidCannotSignUpMessages {
    val title: String = "Your client cannot take part in this pilot"
    val heading: String = "Your client cannot take part in this pilot"
    val para1: String = "You will not be able to take part in this pilot on your client’s behalf if they have ever claimed a coronavirus (COVID-19) grant, or intended to do so in future."
    val para2: String = "You will need to send a Self Assessment tax return instead and you may be able to sign your client up in future."
    val button: String = "Sign up another client"
    val backLink: String = "Back"
    val signOut: String = "Sign out"
  }

  "GET /error/covid-cannot-sign-up" should {
    "return OK" in new Setup {
      result should have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new Setup {
      doc.title shouldBe CovidCannotSignUpMessages.title
    }

    "have a view with the correct heading" in new Setup {
      pageContent.getH1Element.text shouldBe CovidCannotSignUpMessages.heading
    }

    "have a paragraph explaining why they cannot sign up" in new Setup {
      pageContent.getNthParagraph(1).text shouldBe CovidCannotSignUpMessages.para1
    }

    "have a paragraph about sending a Self Assessment" in new Setup {
      pageContent.getNthParagraph(2).text shouldBe CovidCannotSignUpMessages.para2
    }

    "have a back link" in new Setup {
      pageContent.getBackLink.text shouldBe CovidCannotSignUpMessages.backLink
    }

    "have a Sign up another client button" in new Setup {
      val submitButton: Element = pageContent.getForm.getSubmitButton
      submitButton.text shouldBe CovidCannotSignUpMessages.button
      submitButton.attr("class") shouldBe "button"
      submitButton.attr("type") shouldBe "submit"
    }

    "have a Sign Out link" in new Setup {
      val signOutLink: Element = pageContent.getLink("sign-out")
      signOutLink.attr("href") shouldBe controllers.SignOutController.signOut(controllers.agent.eligibility.routes.CovidCannotSignUpController.show().url).url
      signOutLink.text shouldBe CovidCannotSignUpMessages.signOut
    }
  }
}
