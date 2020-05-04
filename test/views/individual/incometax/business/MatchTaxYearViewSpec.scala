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

import assets.MessageLookup.Business.{MatchTaxYear => messages}
import assets.MessageLookup.{Base => common}
import forms.individual.business.MatchTaxYearForm
import forms.submapping.YesNoMapping
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class MatchTaxYearViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall

  implicit val request: Request[_] = FakeRequest()

  private def page(isEditMode: Boolean, isRegistration: Boolean, addFormErrors: Boolean): HtmlFormat.Appendable =
    views.html.individual.incometax.business.match_to_tax_year(
    matchTaxYearForm = MatchTaxYearForm.matchTaxYearForm.addError(addFormErrors),
    postAction = action,
    isRegistration = isRegistration,
    backUrl = backUrl,
    isEditMode
  )(FakeRequest(), implicitly, appConfig)


  "The Match tax year view" when {
    "in subscription mode" should {

      def documentCore(isEditMode: Boolean): TestView =
        TestView(
          name = "Match tax year View",
          title = messages.SignUp.title,
          heading = messages.SignUp.heading,
          page = page(isEditMode = isEditMode, isRegistration = false, addFormErrors = false)
        )

      val testPage = documentCore(isEditMode = false)

      testPage.mustHaveBackLinkTo(backUrl)

      testPage.mustHaveParaSeq(
        messages.SignUp.line1
      )

      val form = testPage.getForm("Match tax year form")(actionCall = action)

      form.mustHaveRadioSet(
        legend = messages.SignUp.heading,
        radioName = MatchTaxYearForm.matchTaxYear
      )(
        YesNoMapping.option_yes -> common.yes,
        YesNoMapping.option_no -> common.no
      )

      form.mustHaveContinueButton()

      "The Match tax year view in edit mode" should {
        val editModePage = documentCore(isEditMode = true)

        editModePage.mustHaveUpdateButton()
      }

      "Append Error to the page title if the form has error" should {
        def documentCore(): TestView = TestView(
          name = "Match tax year View",
          title = titleErrPrefix + messages.SignUp.title,
          heading = messages.SignUp.heading,
          page = page(isEditMode = false, addFormErrors = true, isRegistration = false)
        )

        val testPage = documentCore()
      }
    }

    "in registration mode" should {

      def documentCore(isEditMode: Boolean): TestView = TestView(
          name = "Match tax year View",
          title = messages.Registration.title,
          heading = messages.Registration.heading,
          page = page(isEditMode = isEditMode, isRegistration = true, addFormErrors = false)
        )

      val testPage = documentCore(isEditMode = false)

      testPage.mustHaveBackLinkTo(backUrl)

      testPage.mustHaveParaSeq(
        messages.Registration.line1,
        messages.Registration.line2
      )

      val form = testPage.getForm("Match tax year form")(actionCall = action)

      form.mustHaveRadioSet(
        legend = messages.Registration.heading,
        radioName = MatchTaxYearForm.matchTaxYear
      )(
        YesNoMapping.option_yes -> common.yes,
        YesNoMapping.option_no -> common.no
      )

      form.mustHaveContinueButton()

      "The Match tax year view in edit mode" should {
        val editModePage = documentCore(isEditMode = true)

        editModePage.mustHaveUpdateButton()
      }

      "Append Error to the page title if the form has error" should {
        def documentCore(): TestView = TestView(
          name = "Match tax year View",
          title = titleErrPrefix + messages.Registration.title,
          heading = messages.Registration.heading,
          page = page(isEditMode = false, addFormErrors = true, isRegistration = true)
        )

        val testPage: Unit = documentCore()
      }

    }
  }
}
