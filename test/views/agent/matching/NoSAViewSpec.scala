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

import messagelookup.individual.MessageLookup.NoSA.{Agent => messages}
import messagelookup.individual.MessageLookup.{Base => common}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.agent.matching.ClientNoSa


class NoSAViewSpec extends ViewSpecTrait {
  val action: Call = ViewSpecTrait.testCall
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest
  val clientNoSa: ClientNoSa = app.injector.instanceOf[ClientNoSa]
  lazy val page: HtmlFormat.Appendable = clientNoSa()(request, implicitly, appConfig)

  "The No SA view" should {

    val testPage = TestView(
      name = "No SA View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      isAgent = true,
    )

    testPage.mustHavePara(messages.line1)

    testPage.mustHaveALink(id = "sa-signup", messages.linkText, appConfig.signUpToSaLink)

    testPage.mustHaveSignOutButton(common.signOut, Some(request.path))
  }
}
