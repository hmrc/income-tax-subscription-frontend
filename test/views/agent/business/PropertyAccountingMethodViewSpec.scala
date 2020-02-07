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

package views.agent.business

import agent.assets.MessageLookup.{PropertyAccountingMethod => messages}
import forms.agent.AccountingMethodPropertyForm
import forms.submapping.AccountingMethodMapping
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class PropertyAccountingMethodViewSpec extends ViewSpecTrait {
  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean) = views.html.agent.business.property_accounting_method(
    accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.addError(addFormErrors),
    postAction = action,
    isEditMode,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Property Accounting Method View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The Property accounting method view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    val form = testPage.getForm("Property Accounting Method form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = AccountingMethodPropertyForm.accountingMethodProperty
    )(
      AccountingMethodMapping.option_cash -> messages.cash,
      AccountingMethodMapping.option_accruals -> messages.accruals
    )

    form.mustHaveContinueButton()

  }

  "The Property accounting method view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)
    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {

    def documentCore() = TestView(
      name = "Property Accounting Method View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()

  }

}
