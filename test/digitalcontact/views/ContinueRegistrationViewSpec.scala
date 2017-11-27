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

package digitalcontact.views

import assets.MessageLookup.{PreferencesCallBack => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest

class ContinueRegistrationViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = digitalcontact.views.html.continue_registration(
    postAction = action
  )(request, applicationMessages, appConfig)

  "The Continue Registration view" should {
    val testPage = TestView(
      name = "Continue Registration View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    val form = testPage.getForm("Continue Registration form")(actionCall = action)

    form.mustHaveGoBackButton()

    testPage.mustHaveSignOutLink(text = messages.signOut, origin = request.path)

  }
}
