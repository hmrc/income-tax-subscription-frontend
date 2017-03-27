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
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class FrontPageViewSpec extends ViewSpecTrait {

  lazy val getAction = controllers.routes.HomeController.index()

  lazy val page = views.html.frontpage(
    getAction = getAction
  )(FakeRequest(), applicationMessages, appConfig)

  "The 'Front/Start Page view" should {

    val testPage = TestView(
      name = "Front/Start Page View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHaveSeqParas(
      messages.line_1,
      messages.line_2,
      messages.line_3,
      messages.line_4,
      messages.line_5,
      messages.line_6,
      messages.line_7,
      messages.line_8,
      messages.line_9,
      messages.line_10
    )

    testPage.mustHaveSeqBullets(
      messages.bullet_1,
      messages.bullet_2,
      messages.bullet_3,
      messages.bullet_4,
      messages.bullet_5,
      messages.bullet_6,
      messages.bullet_7,
      messages.bullet_8,
      messages.bullet_9,
      messages.bullet_10,
      messages.bullet_11,
      messages.bullet_12,
      messages.bullet_13
    )

    testPage.mustHaveH2(messages.h2)

    val form = testPage.getForm("Main Income Error form")(actionCall = getAction)

    form.mustHaveSubmitButton(common.signUp)

  }
}
