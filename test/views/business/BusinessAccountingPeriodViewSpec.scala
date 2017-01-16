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

import assets.MessageLookup.{Base => commonMessages}
import assets.MessageLookup.{AccountingPeriod => messages}
import forms.AccountingPeriodForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import util.UnitTestTrait

class BusinessAccountingPeriodViewSpec extends UnitTestTrait {

  lazy val page = views.html.business.accounting_period(
    accountingPeriodForm = AccountingPeriodForm.accountingPeriodForm,
    postAction = controllers.business.routes.BusinessNameController.submitBusinessName()
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Business Accounting Period view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    "has a form" which {

      s"Has a legend with the text '${commonMessages.startDate}'" in {
        document.select("legend:nth-child(1) h2").text() mustBe commonMessages.startDate
      }

    }

  }
}
