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

import assets.MessageLookup.{Terms => messages}
import forms.TermForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class TermsViewSpec extends UnitTestTrait
  with ViewSpecTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  def page(isEditMode: Boolean) = views.html.terms(
    termsForm = TermForm.termForm,
    postAction = controllers.routes.TermsController.submitTerms(),
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Terms view" should {
    val testPage = TestView(
      name = "Terms view",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false))

    testPage.mustHaveBackTo(backUrl)

    testPage.mustHavePara(messages.line_1)

    val form = testPage.getForm("terms form")(method = "POST", action = controllers.routes.TermsController.submitTerms().url)

    form.mustHaveCheckbox(messages.checkbox)

    form.mustHaveContinueButton()

  }

  "When in edit mode, the terms view" should {
    val editModePage = TestView(
      name = "Terms view",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = true))

    editModePage.mustHaveUpdateButton()
  }

}
