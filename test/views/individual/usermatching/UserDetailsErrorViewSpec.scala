/*
 * Copyright 2021 HM Revenue & Customs
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

package views.individual.usermatching

import assets.MessageLookup.{Base => commonMessages, UserDetailsError => messages}
import views.ViewSpecTrait
import views.html.individual.usermatching.UserDetailsError

class UserDetailsErrorViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  implicit val request = ViewSpecTrait.viewTestRequest

  val userDetailsError = app.injector.instanceOf[UserDetailsError]
  lazy val page = userDetailsError(action)(request, implicitly, appConfig)

  "The User Details Error view" should {
    val testPage = TestView(
      name = "User Details Error",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("User Details Error form")(actionCall = action)

    form.mustHaveContinueButtonWithText(commonMessages.goBack)

  }
}
