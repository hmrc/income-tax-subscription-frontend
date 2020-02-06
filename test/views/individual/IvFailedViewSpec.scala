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

package views.individual

import assets.MessageLookup.{IvFailed => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class IvFailedViewSpec extends ViewSpecTrait {
  val testUrl = "link/iv"

  lazy val page = views.html.individual.iv_failed(testUrl)(FakeRequest(), applicationMessages, appConfig)

  "The IV failed view spec" should {
    val testPage = TestView(
      name = "IV failed View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = true
    )

    testPage.mustHavePara(messages.line_1)
    testPage.mustHavePara(messages.line_2)

    testPage.mustHaveALink("contact-hmrc", messages.hmrcLink, appConfig.contactHmrcLink)
    testPage.mustHaveALink("try-again", messages.tryAgainLink, testUrl)
  }
}
