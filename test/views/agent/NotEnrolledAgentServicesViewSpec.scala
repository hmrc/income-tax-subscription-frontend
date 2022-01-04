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

package views.agent

import agent.assets.MessageLookup.{Base => commonMessages, NotEnrolledAgentServices => messages}
import views.ViewSpecTrait
import views.html.agent.NotEnrolledAgentServices

class NotEnrolledAgentServicesViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  val NotEnrolledAgentServicesView: NotEnrolledAgentServices = app.injector.instanceOf[NotEnrolledAgentServices]

  lazy val page = NotEnrolledAgentServicesView()(request, implicitly, appConfig)

  "The Agent not Enrolled to Agent Services view" should {
    val testPage = TestView(
      name = "Agent not Enrolled to Agent Services",
      title = messages.title,
      heading = messages.heading,
      page = page,
      isAgent = true
    )

    testPage.mustHavePara(messages.para1)

    val paragraph1 = testPage.selectHead("return content body", "#main-content").selectHead("paragraph 1", "p")
    paragraph1.mustHaveALink(messages.linkText, appConfig.agentServicesUrl)

    testPage.mustHaveSignOutButton(commonMessages.signOut, request.path)

  }
}
