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

import agent.assets.MessageLookup.{Base => commonMessages, ClientDetailsError => messages}
import views.ViewSpecTrait
import views.html.agent.ClientDetailsError

class ClientDetailsErrorViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val clientDetailsError = app.injector.instanceOf[ClientDetailsError]
  lazy val page = clientDetailsError(action)(request, implicitly, appConfig)



  "The Client Details Error view" should {
    val testPage = TestView(
      name = "Client Details Error",
      title = messages.title,
      heading = messages.heading,
      page = page,
      isAgent = true
    )

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("Client Details Error form")(actionCall = action)

    form.mustHaveContinueButtonWithText(commonMessages.tryAgain)

    testPage.mustHaveSignOutLinkGovUk(commonMessages.signOut, request.path)

  }
}
