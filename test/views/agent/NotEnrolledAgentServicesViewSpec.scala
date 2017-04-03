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

package views.agent

import assets.MessageLookup.AgentMessages.{NotEnrolledAgentServices => messages}
import assets.MessageLookup.{Base => commonMessages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class NotEnrolledAgentServicesViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.agent.not_enrolled_agent_services(action)(FakeRequest(), applicationMessages, appConfig)

  "The Agent not Enrolled to Agent Services view" should {
    val testPage = TestView(
      name = "Agent not Enrolled to Agent Services",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(messages.para1)

    val form = testPage.getForm("Agent not Enrolled to Agent Services form")(actionCall = action)

    form.mustHaveSubmitButton(messages.button)

    testPage.mustHaveALink("sign-out", commonMessages.signOut, controllers.routes.SignOutController.signOut().url)

  }
}
