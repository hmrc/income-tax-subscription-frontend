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

package views.individual.incometax.business

import assets.MessageLookup.{AccountingMethod => messages}
import forms.individual.business.AccountingMethodForm
import forms.submapping.AccountingMethodMapping
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class BusinessAccountingMethodViewSpec extends ViewSpecTrait {
  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.individual.incometax.business.accounting_method(
    accountingMethodForm = AccountingMethodForm.accountingMethodForm.addError(addFormErrors),
    postAction = action,
    isEditMode,
    backUrl = backUrl
  )(FakeRequest(), implicitly, appConfig)

  def documentCore(isEditMode: Boolean): TestView = TestView(
    name = "Business Accounting Method View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )


  "The Business accounting method view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    val accordion = testPage.getAccordion("Business Accounting Method accordion", messages.accordion)

    accordion.mustHaveParaSeq(
      messages.accordion_line_1
    )

    accordion.mustHaveBulletSeq(
      messages.accordion_bullet_1,
      messages.accordion_bullet_2
    )

    val form = testPage.getForm("Business Accounting Method form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = AccountingMethodForm.accountingMethod
    )(
      AccountingMethodMapping.option_cash -> messages.cash,
      AccountingMethodMapping.option_accruals -> messages.accruals
    )

    form.mustHaveContinueButton()

  }

  "The Business accounting method view in edit mode" should {

    val editModePage = documentCore(isEditMode = true)
    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore():TestView = TestView(
      name = "Business Accounting Method View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
