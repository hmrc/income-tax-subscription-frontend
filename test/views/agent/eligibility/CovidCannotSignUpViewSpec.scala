/*
 * Copyright 2022 HM Revenue & Customs
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

package views.agent.eligibility

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.agent.eligibility.CovidCannotSignUp

class CovidCannotSignUpViewSpec extends ViewSpec {

  object CovidCannotSignUpMessages {
    val heading: String = "Your client cannot take part in this pilot"
    val p1 = "You will not be able to take part in this pilot on your client’s behalf if they have ever claimed a coronavirus (COVID-19) grant, or intended to do so in future."
    val p2 = "You will need to send a Self Assessment tax return instead and you may be able to sign your client up in future."
    val saReturnLink = "Self Assessment tax return"
    val signUpAnother = "Sign up another client"
  }

  val covidCannotSignUp: CovidCannotSignUp = app.injector.instanceOf[CovidCannotSignUp]


  "The client who has claimed covid grant before" should {
    "redirect to a covid cannot sign up page with correct view" in {

      new TemplateViewTest(
        view = covidCannotSignUp(
          testCall,
          testBackUrl
        )(request, mockMessages, appConfig),
        title = CovidCannotSignUpMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl)
      )
    }
  }

  "has a paragraph to explain why the client cannot take part in the sign up pilot" in {
    document.getParagraphNth(1).mkString mustBe CovidCannotSignUpMessages.p1
  }

  "has a paragraph for self assessment tax return" in {
    document.getParagraphNth(2).mkString mustBe CovidCannotSignUpMessages.p2
  }

  "has a link for self assessment tax return" in {
    val saReturnLink = document.getLinkNth(5)
    saReturnLink.text() mustBe CovidCannotSignUpMessages.saReturnLink
    saReturnLink.attr("href") mustBe "https://www.gov.uk/self-assessment-tax-returns/sending-return"
  }

  "has a sign up another client button" in {
    val signUpAnotherButton = document.getElementsByClass("govuk-button").last()
    signUpAnotherButton.text mustBe CovidCannotSignUpMessages.signUpAnother
  }

  "has a sign out link" in {
    val signOutLink = document.getElementById("sign-out-button")
    signOutLink.attr("href") mustBe "/report-quarterly/income-and-expenses/sign-up/logout"
  }

  private def page(): Html = {
    covidCannotSignUp(
      testCall,
      testBackUrl
    )(FakeRequest(), implicitly, appConfig)
  }

  private def document(): Document = {
    Jsoup.parse(page().body)
  }

}
