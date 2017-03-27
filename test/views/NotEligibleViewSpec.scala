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

import assets.MessageLookup.{Base => common, Not_Eligible => messages}
import forms.NotEligibleForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class NotEligibleViewSpec extends ViewSpecTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  lazy val page = views.html.not_eligible(
    notEligibleForm = NotEligibleForm.notEligibleForm,
    postAction = controllers.routes.NotEligibleController.submitNotEligible(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  "The Not Eligible view" should {

    val testPage = TestView("Not Eligible View", page)

    testPage.mustHaveBackTo(backUrl)

    testPage.mustHaveTitle(messages.title)

    testPage.mustHaveH1(messages.heading)

    testPage.mustHaveSeqParas(
      messages.line_1,
      messages.line_2,
      messages.line_3
    )

    val form = testPage.getForm("Not Eligible form")(method = "POST", action = controllers.routes.NotEligibleController.submitNotEligible().url)

    form.mustHaveRadioSet(
      legend = messages.question,
      radioName = NotEligibleForm.choice
    )(
      NotEligibleForm.option_signup -> messages.signUp,
      NotEligibleForm.option_signout -> messages.signOut
    )

    form.mustHaveContinueButton()

  }

}
