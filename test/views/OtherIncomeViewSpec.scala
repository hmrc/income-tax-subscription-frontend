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

package views

import assets.MessageLookup.{OtherIncome => messages}
import forms.OtherIncomeForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class OtherIncomeViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean) =  views.html.other_income(
    otherIncomeForm = OtherIncomeForm.otherIncomeForm,
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Other Income View" should {

    val testPage = TestView(
      name = "Other Income View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false))

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.para1)

    testPage.mustHaveBulletSeq(
      messages.bullet1,
      messages.bullet2,
      messages.bullet3,
      messages.bullet4,
      messages.bullet5
    )

    val form = testPage.getForm("Other Income form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = OtherIncomeForm.choice
    )(
      OtherIncomeForm.option_yes -> messages.yes,
      OtherIncomeForm.option_no -> messages.no
    )

    form.mustHaveContinueButton()

    val editModePage = TestView(
      name = "Edit Other Income View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = true))

    editModePage.mustHaveUpdateButton()
  }
}
