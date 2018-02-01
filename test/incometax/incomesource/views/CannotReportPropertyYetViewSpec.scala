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

package incometax.incomesource.views

import assets.MessageLookup.{Base => common, CannotReportPropertyYet => messages}
import core.models.DateModel
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class CannotReportPropertyYetViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  val testDateModel = DateModel("6","4","2018")

  lazy val page = incometax.incomesource.views.html.cannot_report_property_yet(
    postAction = action,
    backUrl = backUrl,
    dateModel = testDateModel)(
    FakeRequest(),
    applicationMessages,
    appConfig
  )

  "The Cannot report property yet view" should {

    val testPage = TestView(
      name = "Cannot report property yet View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHaveParaSeq(
      messages.para1,
      messages.para2(testDateModel)
    )
    testPage.mustHaveALink("sa", messages.linkText, appConfig.signUpToSaLink)

    testPage.mustHaveContinueToSignUpButton()

    testPage.mustHaveSignOutLink(common.signOut)

  }

}
