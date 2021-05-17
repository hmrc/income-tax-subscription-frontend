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

import assets.MessageLookup.{Base => common, UserDetails => messages}
import forms.usermatching.UserDetailsForm
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class UserDetailsViewSpec extends ViewSpecTrait {

  implicit val request: Request[_] = FakeRequest()

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.individual.usermatching.user_details(
    userDetailsForm = UserDetailsForm.userDetailsForm.form.addError(addFormErrors),
    postAction = action,
    isEditMode = isEditMode
  )(FakeRequest(), implicitly, appConfig)

  def documentCore(isEditMode: Boolean): TestView = TestView(
    name = "User Details View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The User Details view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHavePara(messages.line1)

    val form = testPage.getForm("User Details form")(actionCall = action)

    System.out.println(form.element.getAllElements)

    form.mustHaveTextField(
      name = UserDetailsForm.userFirstName,
      label = messages.field1
    )

    form.mustHaveTextField(
      name = UserDetailsForm.userLastName,
      label = messages.field2
    )

    form.mustHaveTextField(
      name = UserDetailsForm.userNino,
      label = messages.field3,
      hint = Some(messages.formhint1_line1)
    )

    form.mustHaveDateField(
      id = "userDateOfBirth",
      legend = common.dateOfBirth,
      exampleDate = messages.formhint2)

    form.mustHaveContinueButton()

  }

  "The User Details view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore():TestView = TestView(
      name = "User Details View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage: Unit = documentCore()
  }
}
