/*
 * Copyright 2020 HM Revenue & Customs
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

package views.helpers

import assets.MessageLookup
import org.jsoup.Jsoup

import utilities.UnitTestTrait
import views.html.helpers.continueButton

class ContinueButtonHelperSpec extends UnitTestTrait {

  val view = continueButton()(implicitly)
  val html = Jsoup.parse(view.body)

  "The continue button helper" should {

    "create a button" which {

      val continueButton = html.select("button")

      "is of tag button" in {
        continueButton.isEmpty mustBe false
      }

      "has the class 'button'" in {
        continueButton.hasClass("button") mustBe true
      }

      "has the type 'submit'" in {
        continueButton.attr("type") mustBe "submit"
      }

      "has the attribute formnovalidate to disable HTML5 validation" in {
        continueButton.hasAttr("formnovalidate") mustBe true
      }

      "has the id of 'continue-button'" in {
        continueButton.attr("id") mustBe "continue-button"
      }

      s"has the value '${MessageLookup.Base.continue}'" in {
        continueButton.text() mustBe MessageLookup.Base.continue
      }
    }
  }
}
