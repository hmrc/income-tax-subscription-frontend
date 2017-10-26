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

package agent.views

import agent.assets.MessageLookup.{Base, MainIncomeError => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class MainIncomeErrorViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  lazy val page = agent.views.html.main_income_error(backUrl, action)(FakeRequest(), applicationMessages, appConfig)

  "The Main Income Error view" should {
    val testPage = TestView(
      name = "Main Income Error View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHaveParaSeq(
      messages.para1,
      messages.para2
    )

    testPage.mustHaveBulletSeq(
      messages.bullet1,
      messages.bullet2,
      messages.bullet3
    )

    val form = testPage.getForm("Main Income Error form")(actionCall = action)

    form.mustHaveSubmitButton(Base.signOut)
  }

}
