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

import assets.MessageLookup.{RentUkProperty => messages}
import forms.individual.incomesource.RentUkPropertyForm
import forms.submapping.YesNoMapping
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class RentUkPropertyViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.individual.incometax.incomesource.rent_uk_property(
    rentUkPropertyForm = RentUkPropertyForm.rentUkPropertyForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  "The Rent Uk Property view" should {
    val testPage = TestView(
      name = "Rent Uk Property View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = false)
    )

    val form = testPage.getForm("Rent Uk Property form")(actionCall = action)

    testPage.mustHavePara(messages.line_1)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = RentUkPropertyForm.rentUkProperty
    )(
      YesNoMapping.option_yes -> messages.yes,
      YesNoMapping.option_no -> messages.no
    )

    form.mustHaveRadioSet(
      legend = messages.question,
      radioName = RentUkPropertyForm.onlySourceOfSelfEmployedIncome
    )(
      YesNoMapping.option_yes -> Messages("base.yes"),
      YesNoMapping.option_no -> Messages("base.no")
    )

    form.mustHaveContinueButton()

  }

  "Append Error to the page title if the form has error" should {
    def documentCore(): TestView = TestView(
      name = "Rent Uk Property View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
