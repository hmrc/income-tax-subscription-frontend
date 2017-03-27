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

import assets.MessageLookup.{AlreadyEnrolled => messages, Base => common}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Call
import play.api.test.FakeRequest

class AlreadyEnrolledViewSpec extends ViewSpecTrait {

  lazy val testPostRoute = "testPostUrl"
  lazy val postAction = Call("POST", testPostRoute)
  lazy val page = views.html.enrolled.already_enrolled(postAction)(FakeRequest(), applicationMessages, appConfig)

  "The Already Enrolled view" should {
    val testPage = TestView(
      name = "Already Enrolled View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(
      messages.para1
    )

    val form = testPage.getForm("Already Enrolled form")(actionCall = postAction)

    form.mustHaveSubmitButton(common.signOut)

  }
}
