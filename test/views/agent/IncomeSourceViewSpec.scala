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

package views.agent

import agent.assets.MessageLookup.{IncomeSource => messages}
import forms.agent.IncomeSourceForm
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class IncomeSourceViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.agent.income_source(
    incomeSourceForm = IncomeSourceForm.incomeSourceForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Income source view" should {
    val testPage: TestView = TestView(
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
      IncomeSourceForm.option_both -> messages.both
    )
    form.mustHaveContinueButton()

  }

  "Append Error to the page title if the form has error" should {
    def documentCore():TestView = TestView(
      name = "Income source View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
