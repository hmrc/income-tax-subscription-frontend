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

package views.individual.incometax.incomesource

import assets.MessageLookup.{AreYouSelfEmployed => messages, Base => coomon}
import forms.individual.incomesource.AreYouSelfEmployedForm
import forms.submapping.YesNoMapping
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class AreYouSelfEmployedViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  implicit val request: Request[_] = FakeRequest()

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.individual.incometax.incomesource.are_you_selfemployed(
    areYouSelfEmployedForm = AreYouSelfEmployedForm.areYouSelfEmployedForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), implicitly, appConfig)

  "The Are you self-employed View" should {

    val testPage = TestView(
      name = "Are you self-employed View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = false))

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHavePara(messages.para1)

    val form = testPage.getForm("Are you self-employed form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = AreYouSelfEmployedForm.choice
    )(
      YesNoMapping.option_yes -> coomon.yes,
      YesNoMapping.option_no -> coomon.no
    )

    form.mustHaveContinueButton()

    val editModePage = TestView(
      name = "Edit Are you self-employed View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = true, addFormErrors = false))

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore(): Unit = TestView(
      name = "Are you self-employed View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage: Unit = documentCore()
  }
}
