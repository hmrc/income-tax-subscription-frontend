/*
 * Copyright 2018 HM Revenue & Customs
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

import agent.assets.MessageLookup.{IncomeSource => messages}
import agent.forms.IncomeSourceForm
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class IncomeSourceViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean) = agent.views.html.income_source(
    incomeSourceForm = IncomeSourceForm.incomeSourceForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Income source view" should {
    val testPage = TestView(
      name = "Income source View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = false)
    )

    val form = testPage.getForm("Income source form")(actionCall = action)
    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = IncomeSourceForm.incomeSource
    )(
      IncomeSourceForm.option_business -> messages.business,
      IncomeSourceForm.option_property -> messages.property,
      IncomeSourceForm.option_both -> messages.both,
      IncomeSourceForm.option_other -> messages.other
    )
    form.mustHaveContinueButton()

  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = "Income source View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
