/*
 * Copyright 2022 HM Revenue & Customs
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

import assets.MessageLookup.{Base => commonMessages, UserDetailsLockout => messages}
import views.ViewSpecTrait
import views.html.individual.usermatching.UserDetailsLockout

class UserDetailsLockoutViewSpec extends ViewSpecTrait {

  val testTime = "test time"
  val request = ViewSpecTrait.viewTestRequest

  val userDetailsLockout = app.injector.instanceOf[UserDetailsLockout]
  lazy val page = userDetailsLockout(testTime)(request, implicitly, appConfig)

  "The User Details Lockout view" should {
    val testPage = TestView(
      name = "User Details Lockout",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(messages.line1(testTime))

    testPage.mustHaveSignOutButton(commonMessages.signOut, Some(request.path))

  }
}
