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

package usermatching.views

import assets.MessageLookup.{Base => common, NoSA => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._

class NoSAViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = usermatching.views.html.no_sa()(request, applicationMessages, appConfig)

  "The No SA view" should {

    val testPage = TestView(
      name = "No SA View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHavePara(messages.line1)

    testPage.mustHaveALink(id = "sa-signup", messages.linkText, appConfig.signUpToSaLink)

    testPage.mustHaveSignOutButton(common.signOut, request.path)

  }
}
