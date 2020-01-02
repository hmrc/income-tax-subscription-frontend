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

package incometax.business.views

import assets.MessageLookup.{BusinessPhoneNumber => messages}
import core.views.ViewSpecTrait
import incometax.business.forms.BusinessPhoneNumberForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class BusinessPhoneNumberViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean) = incometax.business.views.html.business_phone_number(
    businessPhoneNumberForm = BusinessPhoneNumberForm.businessPhoneNumberForm.form.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Business phone number View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The Business phone number view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    val form = testPage.getForm("Business phone number form")(actionCall = action)

    form.mustHaveTextField(
      name = BusinessPhoneNumberForm.phoneNumber,
      label = messages.heading,
      showLabel = false)

    form.mustHaveContinueButton()

  }

  "The Business phone number view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = "Business phone number View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }

}
