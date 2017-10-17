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

package incometax.business.views

import assets.MessageLookup.Business.{RegisterNextAccountingPeriod => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import views.ViewSpecTrait

class BusinessRegisterNextAccountingPeriodViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  lazy val page = incometax.business.views.html.register_next_accounting_period(
    postAction = action,
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

    testPage.mustHaveALink(id = "sign-out", href = core.controllers.routes.SignOutController.signOut().url, text = messages.signOut)

    val form = testPage.getForm("Register Next Accounting Period form")(actionCall = action)

    form.mustHaveContinueToSignUpButton()



  }
}
