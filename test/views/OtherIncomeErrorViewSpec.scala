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

import assets.MessageLookup.{OtherIncomeError => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class OtherIncomeErrorViewSpec extends ViewSpecTrait {

  lazy val backUrl: String = controllers.routes.OtherIncomeController.showOtherIncome().url

  lazy val page = views.html.other_income_error(postAction = controllers.routes.OtherIncomeErrorController.submitOtherIncomeError(), backUrl = backUrl)(FakeRequest(), applicationMessages, appConfig)

  "The Main Income Error view" should {

    val testPage = TestView("Main Income Error View", page)

    testPage.mustHaveBackTo(backUrl)

    testPage.mustHaveTitle(messages.title)

    testPage.mustHaveH1(messages.heading)

    testPage.mustHaveSeqParas(
      messages.para1,
      messages.para2
    )

  }

}

