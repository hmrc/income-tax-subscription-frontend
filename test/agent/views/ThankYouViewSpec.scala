/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.views

import agent.assets.MessageLookup.{ThankYou => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest


class ThankYouViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = agent.views.html.feedback_thank_you()(FakeRequest(), applicationMessages, appConfig)

  "The Thank You Page view" should {

    val testPage = TestView(
      name = "Thank You Page",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHavePara(messages.line_1)

    testPage.mustHaveALink("gotoGovUK", messages.gotoGovUk, "https://www.gov.uk")

  }

}
