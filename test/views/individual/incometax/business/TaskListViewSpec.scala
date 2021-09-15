/*
 * Copyright 2021 HM Revenue & Customs
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

package views.individual.incometax.business

import assets.MessageLookup.TaskList._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.ViewSpecTrait
import views.html.individual.incometax.business.TaskList


class TaskListViewSpec extends ViewSpecTrait {

  val taskListView: TaskList = app.injector.instanceOf[TaskList]
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest

  def document: Document = Jsoup.parse(page().body)

  def page(): Html = taskListView(
    postAction = controllers.individual.subscription.routes.ConfirmationController.submit(),
    viewModel = "something"
  )(request, implicitly, appConfig)

  "business task list view" must {
    "have a title" in {
      document.title mustBe title
    }

    "have a heading" in {
      document.select("h1").text mustBe heading
    }

    "have a subheading" in {
      document.select("h2").text() must include(subHeading)
    }

    "have content" in {
      val paragraphs: Elements = document.select(".govuk-body").select("p")
      paragraphs.text() mustBe contentSummary
    }

    "have a contents list" in {
      val contentList = document.select("ol").select("h2")
      contentList.text() must include(item1)
      contentList.text() must include(item2)
      contentList.text() must include(item3)
    }

    "have a continue button" in {
      val button = document.select("button")
      button.text() must be(continue)
    }
  }
}