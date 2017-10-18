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

import assets.MessageLookup.{Base => common, FrontPage => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class FrontPageViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.frontpage(
    getAction = action
  )(FakeRequest(), applicationMessages, appConfig)

  "The 'Front/Start Page view" should {

    val testPage = TestView(
      name = "Front/Start Page View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHaveParaSeq(
      messages.line_1,
      messages.line_2,
      messages.line_3,
      messages.line_4,
      messages.line_5,
      messages.line_6
    )

    testPage.mustHaveBulletSeq(
      messages.bullet_1,
      messages.bullet_2,
      messages.bullet_3,
      messages.bullet_4,
      messages.bullet_5,
      messages.bullet_6,
      messages.bullet_7,
      messages.bullet_8,
      messages.bullet_9
    )

    testPage.mustHaveH2(messages.subHeading_1)

    testPage.mustHaveH2(messages.subHeading_2)

    val form = testPage.getForm("Main Income Error form")(actionCall = action)

    form.mustHaveSubmitButton(common.signUp)

  }
}
