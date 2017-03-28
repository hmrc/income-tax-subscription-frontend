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

package views.business

import assets.MessageLookup.Business.{RegisterNextAccountingPeriod => messages}
import forms.RegisterNextAccountingPeriodForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class BusinessRegisterNextAccountingPeriodViewSpec extends ViewSpecTrait {

  lazy val backUrl = "BackUrl"

  lazy val postAction = controllers.business.routes.RegisterNextAccountingPeriodController.submit()
  lazy val page = views.html.business.register_next_accounting_period(
    registerNextAccountingPeriodForm = RegisterNextAccountingPeriodForm.registerNextAccountingPeriodForm,
    postAction = postAction,
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  "The 'Register Next Accounting Period' view" should {
    val testPage = TestView(
      name = "Register Next Accounting Period View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHaveBackLinkTo(backUrl)

    testPage.mustHaveParaSeq(
      messages.line_1,
      messages.line_2
    )

    val form = testPage.getForm("Register Next Accounting Period form")(actionCall = postAction)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = RegisterNextAccountingPeriodForm.registerNextAccountingPeriod
    )(
      RegisterNextAccountingPeriodForm.option_yes -> messages.yes,
      RegisterNextAccountingPeriodForm.option_no -> messages.no
    )

    form.mustHaveContinueButton()

  }
}
