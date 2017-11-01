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

package agent.views

import agent.assets.MessageLookup.{OtherIncome => messages}
import agent.forms.{IncomeSourceForm, OtherIncomeForm}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class OtherIncomeViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, incomeSource: String) = agent.views.html.other_income(
    otherIncomeForm = OtherIncomeForm.otherIncomeForm,
    incomeSource = incomeSource,
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Other Income View" should {

    val testPageProperty = TestView(
      name = "Other Income View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, incomeSource = IncomeSourceForm.option_property))

    testPageProperty.mustHaveBackLinkTo(backUrl)

    testPageProperty.mustHavePara(messages.para1)

    testPageProperty.mustHaveBulletSeq(
      messages.bullet1Property,
      messages.bullet2,
      messages.bullet3,
      messages.bullet4,
      messages.bullet5
    )

    val form = testPageProperty.getForm("Other Income form")(actionCall = action)

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
      page = page(isEditMode = true, incomeSource = IncomeSourceForm.option_business))

    // n.b. bullet1Default test displays for income sources that include a business (property only used bullet1Property
    editModePage.mustHaveBulletSeq(
      messages.bullet1Default,
      messages.bullet2,
      messages.bullet3,
      messages.bullet4,
      messages.bullet5
    )

    editModePage.mustHaveUpdateButton()
  }
}
