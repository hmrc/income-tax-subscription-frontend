/*
 * Copyright 2020 HM Revenue & Customs
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

package views.agent

import agent.assets.MessageLookup.{Base => common, ClientDetails => messages}
import forms.agent.ClientDetailsForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class ClientDetailsViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean) = views.html.agent.client_details(
    clientDetailsForm = ClientDetailsForm.clientDetailsForm.form.addError(addFormErrors),
    postAction = action,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Client Details View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The Client Details view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("Client Details form")(actionCall = action)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientFirstName,
      label = messages.field1)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientLastName,
      label = messages.field2)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientNino,
      label = messages.field3,
      hint = messages.formhint1_line1)

    form.mustHaveDateField(
      id = "clientDateOfBirth",
      legend = common.dateOfBirth,
      exampleDate = messages.formhint2)

    form.mustHaveContinueButton()

  }

  "The Client Details view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = "Client Details View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
