/*
 * Copyright 2017 HM Revenue & Customs
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

package views

import assets.MessageLookup.{Terms => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class TermsViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  def page() = views.html.terms(
    postAction = action,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  "The Terms view" should {
    val testPage = TestView(
      name = "Terms view",
      title = messages.title,
      heading = messages.heading,
      page = page())

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.line_1)
    testPage.mustHavePara(messages.line_2)

    testPage.mustHaveBulletSeq(
      messages.bullet_1,
      messages.bullet_2,
      messages.bullet_3,
      messages.bullet_4,
      messages.bullet_5,
      messages.bullet_6,
      messages.bullet_7
    )

    val form = testPage.getForm("terms form")(actionCall = action)

    form.mustHaveSubmitButton(messages.button)
  }

}
