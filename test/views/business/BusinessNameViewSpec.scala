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

package views.business

import assets.MessageLookup.{BusinessName => messages}
import forms.BusinessNameForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class BusinessNameViewSpec extends ViewSpecTrait {

  lazy val backUrl = controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url

  lazy val postAction = controllers.business.routes.BusinessNameController.submitBusinessName()

  def page(isEditMode: Boolean) = views.html.business.business_name(
    businessNameForm = BusinessNameForm.businessNameForm,
    postAction = postAction,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Business Name View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode)
  )

  "The Business Name view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.line_1)

    val form = testPage.getForm("Business Name form")(actionCall = postAction)

    form.mustHaveTextField(
      name = BusinessNameForm.businessName,
      label = messages.heading,
      showLabel = false)

    form.mustHaveContinueButton()

  }

  "The Business Name view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

}
