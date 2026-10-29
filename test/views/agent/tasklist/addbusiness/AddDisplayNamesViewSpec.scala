/*
 * Copyright 2026 HM Revenue & Customs
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

package views.agent.tasklist.addbusiness

import forms.common.DisplayNameForm
import models.common.SoleTraderBusiness
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utilities.ViewSpec
import views.html.agent.tasklist.addbusiness.AddDisplayNames

class AddDisplayNamesViewSpec extends ViewSpec {

  private val addDisplayNames: AddDisplayNames = app.injector.instanceOf[AddDisplayNames]
  private val businesses = Seq(
    SoleTraderBusiness(id = "business-1", name = Some("Business one"), trade = Some("Trade one")),
    SoleTraderBusiness(id = "business-2", name = Some("Business two"), trade = Some("Trade two"))
  )
  private val page: HtmlFormat.Appendable = addDisplayNames(
    DisplayNameForm.displayNameForm,
    testCall,
    businesses
  )
  private val pageDocument: Document = Jsoup.parse(page.body)

  "AddDisplayNames" should {
    "use the agent template and display the page heading" in new TemplateViewTest(
      view = page,
      title = AgentDisplayNamesContent.title,
      isAgent = true
    ) {
      pageDocument.mainContent.getH1Element.text mustBe AgentDisplayNamesContent.heading
    }

    "display each business and a corresponding display name field" in {
      val main = pageDocument.mainContent

      main.selectSeq("h2").map(_.text) mustBe Seq(
        AgentDisplayNamesContent.subHeading(1),
        AgentDisplayNamesContent.subHeading(2)
      )
      main.text must include("Business one")
      main.text must include("Trade one")
      main.text must include("Business two")
      main.text must include("Trade two")
      main.selectSeq("input").map(_.attr("name")) must contain theSameElementsAs Seq(
        "business-display-name[0]",
        "business-display-name[1]"
      )
      main.selectHead("form").attr("action") mustBe testCall.url
      main.selectHead("button[type=submit]").text mustBe AgentDisplayNamesContent.saveAndContinue
    }
  }

  private object AgentDisplayNamesContent {
    val title = "Create display labels for your client’s income sources"
    val heading = "Create display labels for your client’s income sources"
    val saveAndContinue = "Save and continue"
    def subHeading(index: Int): String = s"Sole trader business $index"
  }
}
