/*
 * Copyright 2023 HM Revenue & Customs
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

package views.agent.matching

import messagelookup.agent.MessageLookup.{Base => common, ClientDetailsError => messages}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.agent.matching.ClientDetailsError

class ClientDetailsErrorViewSpec extends ViewSpecTrait {

  val action: Call = ViewSpecTrait.testCall
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest

  lazy val clientDetailsError: ClientDetailsError = app.injector.instanceOf[ClientDetailsError]
  lazy val page: HtmlFormat.Appendable = clientDetailsError(action)(request, implicitly)


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

    form.mustHaveContinueButtonWithText(common.tryAgain)

    testPage.mustHaveSignOutLinkGovUk(common.signOut, Some(request.path))

  }
}
