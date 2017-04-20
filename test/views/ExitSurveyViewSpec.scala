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
import forms.ExitSurveyForm
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest


class ExitSurveyViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = views.html.exit_survey(
    exitSurveyForm = ExitSurveyForm.exitSurveyForm,
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

    form.mustHaveRadioSet(
      messages.Q1.question,
      ExitSurveyForm.aboutToQuery,
      useTextForValue = true
    )(
      "1" -> messages.Q1.option_1,
      "2" -> messages.Q1.option_2
    )

    form.mustHaveCheckboxSet(
      messages.Q2.question,
      ExitSurveyForm.additionalTasks,
      useTextForValue = true
    )(
      messages.Q2.option_1,
      messages.Q2.option_2,
      messages.Q2.option_3,
      messages.Q2.option_4,
      messages.Q2.option_5,
      messages.Q2.option_6,
      messages.Q2.option_7
    )

    form.mustHaveRadioSet(
      messages.Q3.question,
      ExitSurveyForm.experience,
      useTextForValue = true
    )(
      "1" -> messages.Q3.option_1,
      "2" -> messages.Q3.option_2,
      "3" -> messages.Q3.option_3,
      "4" -> messages.Q3.option_4,
      "5" -> messages.Q3.option_5
    )

    form.mustHaveRadioSet(
      messages.Q4.question,
      ExitSurveyForm.recommendation,
      useTextForValue = true
    )(
      "1" -> messages.Q4.option_1,
      "2" -> messages.Q4.option_2,
      "3" -> messages.Q4.option_3,
      "4" -> messages.Q4.option_4,
      "5" -> messages.Q4.option_5
    )

    testPage.mustHaveH3(
      messages.line_1
    )

    form.mustHaveSubmitButton(messages.submit)

  }

}
