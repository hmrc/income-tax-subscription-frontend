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

import assets.MessageLookup.{AccountingPeriod => messages, Base => commonMessages}
import forms.AccountingPeriodForm
import models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView, NextAccountingPeriodView}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class BusinessAccountingPeriodViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  def page(viewType: AccountingPeriodViewType, isEditMode: Boolean) = views.html.business.accounting_period(
    accountingPeriodForm = AccountingPeriodForm.accountingPeriodForm,
    postAction = controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod(),
    backUrl = backUrl,
    viewType = viewType,
    isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(viewType: AccountingPeriodViewType, isEditMode: Boolean = false) = Jsoup.parse(page(viewType, isEditMode).body)

  "The Business Accounting Period view" should {
    Seq(CurrentAccountingPeriodView, NextAccountingPeriodView).foreach {
      viewType =>

        val prefix = s"When the viewtype=$viewType "

        lazy val document = documentCore(viewType)

        s"$prefix have a back button pointed to $backUrl" in {
          val backLink = document.select("#back")
          backLink.isEmpty mustBe false
          backLink.attr("href") mustBe backUrl
        }

        s"$prefix have the title '${messages.title}'" in {
          document.title() mustBe messages.title
        }

        val expectedHeading = viewType match {
          case CurrentAccountingPeriodView => messages.heading_current
          case NextAccountingPeriodView => messages.heading_next
        }
        s"$prefix have the heading (H1) '$expectedHeading'" in {

          document.select("h1").text() mustBe expectedHeading
        }

        s"$prefix have the line_1 (P) '${messages.line_1}'" in {
          document.select("p").text() must include(messages.line_1)
        }

        s"$prefix has a form" which {

          s"Has a legend with the text '${commonMessages.startDate}'" in {
            document.select("#startDate legend span.form-label-bold").text() mustBe commonMessages.startDate
          }

          s"Has a legend with the text '${commonMessages.endDate}'" in {
            document.select("#endDate legend span.form-label-bold").text() mustBe commonMessages.endDate
          }

          "has a continue button" in {
            document.select("#continue-button").isEmpty mustBe false
          }

          s"has a post action to '${controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod().url}'" in {
            document.select("form").attr("action") mustBe controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod().url
            document.select("form").attr("method") mustBe "POST"
          }

          "say update" in {
            lazy val documentEdit = documentCore(viewType, isEditMode = true)
            documentEdit.select("#continue-button").text() mustBe "Update"
          }

        }


    }
  }
}
