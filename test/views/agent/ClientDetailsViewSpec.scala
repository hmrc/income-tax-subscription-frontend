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

package views.agent

import assets.MessageLookup.{Base => common}
import assets.MessageLookup.Agent.{ClientDetails => messages}
import forms.agent.ClientDetailsForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class ClientDetailsViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean) = views.html.agent.client_details(
    clientDetailsForm = ClientDetailsForm.clientDetailsForm.form,
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Client Details View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode)
  )

  "The Client Details view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("Client Details form")(actionCall = action)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientFirstName,
      label = messages.field1,
      showLabel = true)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientLastName,
      label = messages.field2,
      showLabel = true)

    form.mustHaveTextField(
      name = ClientDetailsForm.clientNino,
      label = messages.field3,
      hint = messages.formhint1,
      showLabel = true)

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

}
