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

import assets.MessageLookup.{AccountingMethod => messages}
import forms.AccountingMethodForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class BusinessAccountingMethodViewSpec extends ViewSpecTrait {
  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean) = views.html.business.accounting_method(
    accountingMethodForm = AccountingMethodForm.accountingMethodForm,
    postAction = action,
    backUrl = backUrl,
    isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) = TestView(
    name = "Business Accounting Method View",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode)
  )


  "The Business accounting method view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)

    val accordion = testPage.getAccordion("Business Accounting Method accordion", messages.accordion)

    accordion.mustHaveParaSeq(
      messages.accordion_line_1,
      messages.accordion_line_2
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
      AccountingMethodForm.option_cash -> messages.cash,
      AccountingMethodForm.option_accruals -> messages.accruals
    )

    form.mustHaveContinueButton()

  }

  "The Business accounting method view in edit mode" should {

    val editModePage = documentCore(isEditMode = true)
    editModePage.mustHaveUpdateButton()
  }

}
