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

package incometax.unauthorisedagent.views

import assets.MessageLookup.UnauthorisedAgent.{AgentNotAuthorised => messages}
import assets.MessageLookup.{Base => common}
import core.utils.TestConstants._
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._


class AgentNotAuthorisedViewSpec extends ViewSpecTrait {

  val request = ViewSpecTrait.viewTestRequest

  lazy val page = incometax.unauthorisedagent.views.html.agent_not_authorised()(request, applicationMessages, appConfig)

  "The Affinity Group Error view" should {

    val testPage = TestView(
      name = "Agent Not Authorised view",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHavePara(messages.line_1)
    testPage.mustHaveSignOutButton(common.signOut, request.path)

  }
}
