/*
 * Copyright 2023 HM Revenue & Customs
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

package views.individual.claimenrolment

import models.individual.claimenrolment.ClaimEnrolmentOrigin
import models.individual.claimenrolment.ClaimEnrolmentOrigin.{ClaimEnrolmentBTA, ClaimEnrolmentPTA, ClaimEnrolmentSignUp}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.individual.claimenrolment.AddMTDITOverview

class AddMTDITOverviewSpec extends ViewSpec {

  val addMTDITOverview: AddMTDITOverview = app.injector.instanceOf[AddMTDITOverview]

  def page(origin: ClaimEnrolmentOrigin): HtmlFormat.Appendable = addMTDITOverview(testCall, origin)

  def document(origin: ClaimEnrolmentOrigin): Document = Jsoup.parse(page(origin).body)


  "AddMTDITOverview" when {
    "the origin is BTA" must {
      lazy val mainContent: Element = document(ClaimEnrolmentBTA).mainContent

      "use the correct page template" in new TemplateViewTest(
        view = page(ClaimEnrolmentBTA),
        title = AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.bta),
        hasSignOutLink = true
      )

      "have a heading" in {
        mainContent.getH1Element.text mustBe AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.bta)
      }

      "have a initial paragraph" in {
        mainContent.getNthParagraph(1).text mustBe AddMTDITOverviewMessages.paraOne(AddMTDITOverviewMessages.Origins.bta)
      }

      "have a secondary paragraph" in {
        mainContent.getNthParagraph(2).text mustBe AddMTDITOverviewMessages.paraTwo
      }

      "have a form" which {
        def form: Element = mainContent.getForm

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }
        "has a continue button" in {
          form.getGovukSubmitButton.text mustBe AddMTDITOverviewMessages.continue
        }
      }
    }

    "the origin is PTA" must {
      lazy val mainContent: Element = document(ClaimEnrolmentPTA).mainContent

      "use the correct page template" in new TemplateViewTest(
        view = page(ClaimEnrolmentPTA),
        title = AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.pta),
        hasSignOutLink = true
      )

      "have a heading" in {
        mainContent.getH1Element.text mustBe AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.pta)
      }

      "have a initial paragraph" in {
        mainContent.getNthParagraph(1).text mustBe AddMTDITOverviewMessages.paraOne(AddMTDITOverviewMessages.Origins.pta)
      }

      "have a secondary paragraph" in {
        mainContent.getNthParagraph(2).text mustBe AddMTDITOverviewMessages.paraTwo
      }

      "have a form" which {
        def form: Element = mainContent.getForm

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }
        "has a continue button" in {
          form.getGovukSubmitButton.text mustBe AddMTDITOverviewMessages.continue
        }
      }
    }

    "the origin is sign up" must {
      lazy val mainContent: Element = document(ClaimEnrolmentSignUp).mainContent

      "use the correct page template" in new TemplateViewTest(
        view = page(ClaimEnrolmentSignUp),
        title = AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.signUp),
        hasSignOutLink = true
      )

      "have a heading" in {
        mainContent.getH1Element.text mustBe AddMTDITOverviewMessages.heading(AddMTDITOverviewMessages.Origins.signUp)
      }

      "have a initial paragraph" in {
        mainContent.getNthParagraph(1).text mustBe AddMTDITOverviewMessages.paraOne(AddMTDITOverviewMessages.Origins.signUp)
      }

      "have a secondary paragraph" in {
        mainContent.getNthParagraph(2).text mustBe AddMTDITOverviewMessages.paraTwo
      }

      "have a form" which {
        def form: Element = mainContent.getForm

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }
        "has a continue button" in {
          form.getGovukSubmitButton.text mustBe AddMTDITOverviewMessages.continue
        }
      }
    }
  }

  object AddMTDITOverviewMessages {
    def heading(origin: String): String = s"Add Making Tax Digital for Income Tax to your $origin"

    def paraOne(origin: String) = s"You can now add Making Tax Digital for Income Tax to your $origin and manage it with other taxes."

    val paraTwo = "You may be asked to provide further proof of your identity."
    val continue: String = "Continue"

    object Origins {
      val bta: String = "business tax account"
      val pta: String = "personal tax account"
      val signUp: String = "online services account"
    }
  }

}
