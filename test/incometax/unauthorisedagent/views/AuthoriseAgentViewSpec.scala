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

package incometax.unauthorisedagent.views

import assets.MessageLookup.UnauthorisedAgent.{AuthoriseAgent => messages}
import core.views.ViewSpecTrait
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class AuthoriseAgentViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  def page(addFormErrors: Boolean) = incometax.unauthorisedagent.views.html.authorise_agent(
    authoriseAgentForm = ConfirmAgentForm.confirmAgentForm.addError(addFormErrors),
    postAction = action
  )(FakeRequest(), applicationMessages, appConfig)

  "The Authorise Agent View" should {

    val testPage = TestView(
      name = "Authorise Agent View",
      title = messages.title,
      heading = messages.heading,
      page = page(addFormErrors = false))

    val form = testPage.getForm("Authorise Agent form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = ConfirmAgentForm.choice
    )(
      ConfirmAgentForm.option_yes -> messages.yes,
      ConfirmAgentForm.option_no -> messages.no
    )

    form.mustHaveContinueButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = "Authorise Agent View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
