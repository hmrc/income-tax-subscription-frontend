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

package views.individual.incometax.subscription

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.subscription.SignUpConfirmation

class SignUpConfirmationViewSpec extends ViewSpec {
  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  def page(): Html = signUpConfirmation()

  def document(): Document = Jsoup.parse(page().body)

  "The sign up confirmation view" must {
    "have a heading" in {
      document().selectHead("h1").text() mustBe SignUpConfirmationMessages.heading
    }
  }

  private object SignUpConfirmationMessages {
    val heading = "Sign up complete"
  }
}
