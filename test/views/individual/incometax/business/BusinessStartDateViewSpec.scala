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

package views.individual.incometax.business

import assets.MessageLookup.{Base => common, BusinessStartDate => messages}
import forms.individual.business.BusinessStartDateForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class BusinessStartDateViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable = views.html.individual.incometax.business.business_start_date(
    businessStartDateForm = BusinessStartDateForm.businessStartDateForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(prefix: String, suffix: Option[String] = None, isEditMode: Boolean): TestView = TestView(
    name = s"$prefix Business Start Date View${suffix.fold("")(x => x)}",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The Business Start Date view" should {

    val testPage = documentCore(
      prefix = "Business Start Date view",
      isEditMode = false
    )

    testPage.mustHaveBackLinkTo(backUrl)

    val form = testPage.getForm(s"Business Start Date form")(actionCall = action)

    form.mustHaveDateField(
      id = "startDate",
      legend = common.startDate,
      exampleDate = messages.exampleStartDate
    )

    val editModePage = documentCore(
      prefix = "Business Start Date view",
      suffix = " and it is in edit mode",
      isEditMode = true
    )

    editModePage.mustHaveUpdateButton()

  }

  "Append Error to the page title if the form has error" should {
    def documentCore(): TestView = TestView(
      name = s"Business Start Date View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }
}
