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

import messagelookup.agent.MessageLookup.{Base => common, ClientAlreadySubscribed => messages}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.agent.matching.ClientAlreadySubscribed

class ClientAlreadySubscribedViewSpec extends ViewSpecTrait {

  val action: Call = ViewSpecTrait.testCall
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest
  val clientAlreadySubscribed: ClientAlreadySubscribed = app.injector.instanceOf[ClientAlreadySubscribed]

  lazy val page: HtmlFormat.Appendable = clientAlreadySubscribed(action)(request, implicitly)

  "The Client Already Enrolled view" should {
    val testPage = TestView(
      name = "Client Already Enrolled View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      isAgent = true
    )

    testPage.mustHavePara(
      messages.para1,
      messages.uList,
      messages.bullet1,
      messages.bullet2
    )

    val form = testPage.getForm("Client Already Enrolled form")(actionCall = action)

    form.mustHaveSubmitButton(common.goBack)

    testPage.mustHaveSignOutLinkGovUk(common.signOut, Some(request.path))

  }
}
