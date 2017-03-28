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

package views.throttling

import assets.MessageLookup.{Base => common, ThrottleLimit => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class ThrottleLimitViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.throttling.daily_limit_reached(postAction = action)(FakeRequest(), applicationMessages, appConfig)

  "The Throttle Limit view" should {
    val testPage = TestView(
      name = "Throttle Limit View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(
      messages.line1
    )

    val form = testPage.getForm("Already Enrolled form")(actionCall = action)

    form.mustHaveSubmitButton(common.signOut)

  }

}
