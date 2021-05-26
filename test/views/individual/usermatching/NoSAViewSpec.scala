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

package views.individual.usermatching

import assets.MessageLookup.{Base => common, NoSA => messages}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class NoSAViewSpec extends ViewSpecTrait {

  val action: Call = ViewSpecTrait.testCall
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest

  lazy val page: HtmlFormat.Appendable = views.html.individual.usermatching.no_sa()(request, implicitly, appConfig)

  "The No SA view" should {

    val testPage = TestView(
      name = "No SA View",
      title = messages.title,
      heading = messages.heading,
      page = page,
    )

    testPage.mustHavePara(messages.line1)

    testPage.mustHaveALink(id = "sa-signup", messages.linkText, appConfig.signUpToSaLink)

    testPage.mustHaveSignOutButton(common.signOut, request.path)

  }
}
