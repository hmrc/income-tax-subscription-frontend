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

package views.agent

import agent.assets.MessageLookup.{Base => common, ClientAlreadySubscribed => messages}
import views.ViewSpecTrait

class ClientAlreadySubscribedViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = views.html.agent.client_already_subscribed(action)(request, implicitly, appConfig)

  "The Client Already Enrolled view" should {
    val testPage = TestView(
      name = "Client Already Enrolled View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(
      messages.para1
    )

    val form = testPage.getForm("Client Already Enrolled form")(actionCall = action)

    form.mustHaveSubmitButton(common.goBack)

    testPage.mustHaveSignOutLink(common.signOut, request.path)

  }
}
