/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.views

import agent.assets.MessageLookup.{Base => common, ClientCannotReportPropertyYet => messages}
import core.models.DateModel
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._

class ClientCannotReportPropertyYetViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = agent.views.html.client_cannot_report_property_yet(
    postAction = action,
    backUrl = backUrl)(
    request,
    applicationMessages,
    appConfig
  )

  "The Client Cannot report property yet view" should {

    val testPage = TestView(
      name = "Client Cannot report property yet View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHaveParaSeq(
      messages.para1,
      messages.para2
    )
    testPage.mustHaveALink("sa", messages.linkText, appConfig.signUpToSaLink)

    testPage.mustHaveContinueToSignUpButton()

    testPage.mustHaveSignOutLink(common.signOut, request.path)

  }

}
