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

package views.agent.business

import agent.assets.MessageLookup.{OverseasPropertyAccountingMethod => messages}
import forms.agent.AccountingMethodOverseasPropertyForm
import forms.submapping.AccountingMethodMapping
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class OverseasPropertyAccountingMethodViewSpec extends ViewSpecTrait {
  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.agent.business.overseas_property_accounting_method(
    accountingMethodOverseasPropertyForm = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.addError(addFormErrors),
    postAction = action,
    isEditMode,
    backUrl = backUrl
  )(FakeRequest(), implicitly, appConfig)

  def documentCore(isEditMode: Boolean): TestView = TestView(
    name = "Overseas Property Accounting Method View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The overseas property accounting method view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    val form = testPage.getForm("Overseas Property Accounting Method form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = AccountingMethodOverseasPropertyForm.accountingMethodOverseasProperty
    )(
      AccountingMethodMapping.option_cash -> messages.cash,
      AccountingMethodMapping.option_accruals -> messages.accruals
    )

    form.mustHaveContinueButton()

  }

  "The overseas property accounting method view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)
    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {

    def documentCore(): TestView = TestView(
      name = "Overseas Property Accounting Method View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()

  }

}
