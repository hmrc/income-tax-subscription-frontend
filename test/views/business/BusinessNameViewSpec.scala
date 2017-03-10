/*
 * Copyright 2017 HM Revenue & Customs
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

package views.business

import assets.MessageLookup.{BusinessName => messages}
import forms.BusinessNameForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class BusinessNameViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url

  def page(isEditMode: Boolean) = views.html.business.business_name(
    businessNameForm = BusinessNameForm.businessNameForm,
    postAction = controllers.business.routes.BusinessNameController.submitBusinessName(),
    backUrl = backUrl,
    isEditMode
  )(FakeRequest(), applicationMessages, appConfig)
  def documentCore(isEditMode: Boolean) = Jsoup.parse(page(isEditMode).body)

  "The Business Name view" should {

    lazy val document = documentCore(isEditMode = false)

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document.select("#back")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    s"have the line_1 (P) '${messages.line_1}'" in {
      document.select("p").text() must include(messages.line_1)
    }

    "has a form" which {

      "has a text input field for the business name" in {
        document.select("input[name=BusinessName]").isEmpty mustBe false
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.business.routes.BusinessNameController.submitBusinessName().url}'" in {
        document.select("form").attr("action") mustBe controllers.business.routes.BusinessNameController.submitBusinessName().url
        document.select("form").attr("method") mustBe "POST"
      }

      "say update" in {
        lazy val documentEdit = documentCore(isEditMode = true)
        documentEdit.select("#continue-button").text() mustBe "Update"
      }

    }

  }
}
