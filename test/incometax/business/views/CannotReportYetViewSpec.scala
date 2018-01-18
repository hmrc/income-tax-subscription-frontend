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

package incometax.business.views

import assets.MessageLookup.{Base => common, CannotReportYet => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class CannotReportYetViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  lazy val page = incometax.business.views.html.cannot_report_yet(
    postAction = action,
    backUrl = backUrl)(
    FakeRequest(),
    applicationMessages,
    appConfig
  )

  "The Cannot report yet view" should {

    val testPage = TestView(
      name = "Cannot report yet View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHaveParaSeq(
      messages.para1,
      messages.para2
    )

    testPage.mustHaveSignOutLink(common.signOut)

    testPage.mustHaveALink("sa", messages.linkText, appConfig.signUpToSaLink)
  }

}
