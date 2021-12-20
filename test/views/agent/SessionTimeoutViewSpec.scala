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

package views.agent

import agent.assets.MessageLookup.{Timeout => messages}
import play.api.test.FakeRequest
import views.ViewSpecTrait
import views.html.agent.Timeout

class SessionTimeoutViewSpec extends ViewSpecTrait {

  lazy val page = app.injector.instanceOf[Timeout]

  "The Session timeout view" should {

    val testPage = TestView(
      name = "Session timeout view",
      title = messages.title,
      heading = messages.heading,
      page = page()(FakeRequest(), implicitly, appConfig),
      isAgent = true,
      showSignOutInBanner = false)

    testPage.mustHavePara(messages.returnToHome)

    val para = testPage.selectHead("return content body", "#main-content").selectHead("return home paragraph", "p")
    para.mustHaveALink("sign in", controllers.agent.routes.HomeController.index().url)
  }

}
