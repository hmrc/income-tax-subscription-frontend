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

import assets.MessageLookup.{Base => common, ExitSurvey => messages}
import incometax.subscription.forms.ExitSurveyForm
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest


class ExitSurveyViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.exit_survey(
    exitSurveyForm = ExitSurveyForm.exitSurveyForm.form,
    postAction = action
  )(FakeRequest(), applicationMessages, appConfig)

  "The Exit Survey Page view" should {

    val testPage = TestView(
      name = "Exit Survey Page",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    val form = testPage.getForm("Main Income Error form")(actionCall = action)

    form.mustHaveH3(messages.Q1.question)
    form.mustHaveRadioSet(
      messages.Q1.question,
      ExitSurveyForm.satisfaction,
      useTextForValue = true
    )(
      "1" -> messages.Q1.option_1,
      "2" -> messages.Q1.option_2,
      "3" -> messages.Q1.option_3,
      "4" -> messages.Q1.option_4,
      "5" -> messages.Q1.option_5
    )

    form.mustHaveH3(messages.Q2.question)
    form.mustHaveTextArea(
      ExitSurveyForm.improvements,
      maxLength = 1200
    )

    testPage.mustHavePara(
      messages.line_1
    )

    form.mustHaveSubmitButton(messages.submit)

  }

}
