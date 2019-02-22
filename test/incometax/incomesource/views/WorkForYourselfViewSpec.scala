/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.incomesource.views

import assets.MessageLookup.{Base => coomon, WorkForYourself => messages}
import core.forms.submapping.YesNoMapping
import core.views.ViewSpecTrait
import incometax.incomesource.forms.WorkForYourselfForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class WorkForYourselfViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl

  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean) = incometax.incomesource.views.html.work_for_yourself(
    workForYourselfForm = WorkForYourselfForm.workForYourselfForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Work for Yourself View" should {

    val testPage = TestView(
      name = "Work for Yourself View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = false))

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.para1)

    val form = testPage.getForm("Work for Yourself form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = WorkForYourselfForm.choice
    )(
      YesNoMapping.option_yes -> coomon.yes,
      YesNoMapping.option_no -> coomon.no
    )

    form.mustHaveContinueButton()

    val editModePage = TestView(
      name = "Edit Work for Yourself View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = true, addFormErrors = false))

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = "Work for Yourself View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
