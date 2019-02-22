/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.views.business

import agent.assets.MessageLookup.{AccountingPeriod => messages, Base => common}
import agent.forms.AccountingPeriodDateForm
import agent.models.enums.{AccountingPeriodViewType, CurrentAccountingPeriodView, NextAccountingPeriodView}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class BusinessAccountingPeriodDateViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(viewType: AccountingPeriodViewType, isEditMode: Boolean, addFormErrors: Boolean) = agent.views.html.business.accounting_period_date(
    accountingPeriodForm = AccountingPeriodDateForm.accountingPeriodDateForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    viewType = viewType,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(prefix: String, suffix: Option[String] = None, viewType: AccountingPeriodViewType, isEditMode: Boolean) = TestView(
    name = s"$prefix Business Accounting Period Date View${suffix.fold("")(x => x)}",
    title = messages.title,
    heading = (isEditMode, viewType) match {
      case (true, _) => messages.heading_editMode
      case (_, CurrentAccountingPeriodView) => messages.heading_current
      case (_, NextAccountingPeriodView) => messages.heading_next
    },
    page = page(viewType = viewType, isEditMode = isEditMode, addFormErrors = false)
  )

  "The Business Accounting Period Date view" should {
    Seq(CurrentAccountingPeriodView, NextAccountingPeriodView).foreach {
      viewType =>

        val prefix = s"When the viewtype=$viewType"

        val testPage = documentCore(
          prefix = prefix,
          viewType = viewType,
          isEditMode = false
        )

        testPage.mustHaveBackLinkTo(backUrl)

        val form = testPage.getForm(s"$prefix Business Accounting Period Date form")(actionCall = action)

        form.mustHaveDateField(
          id = "startDate",
          legend = common.startDate,
          exampleDate =
            viewType match {
              case CurrentAccountingPeriodView => messages.exampleStartDate_current
              case _ => messages.exampleStartDate_next
            }
        )

        form.mustHaveDateField(
          id = "endDate",
          legend = common.endDate,
          exampleDate =
            viewType match {
              case CurrentAccountingPeriodView => messages.exampleEndDate_current
              case _ => messages.exampleEndDate_next
            }
        )

        val editModePage = documentCore(
          prefix = prefix,
          suffix = " and it is in edit mode",
          viewType = viewType,
          isEditMode = true
        )

        editModePage.mustHaveUpdateButton()

    }
  }

  "Append Error to the page title if the form has error" should {

    def documentCore() = TestView(
      name = s"Business Accounting Period Date View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading_current,
      page = page(viewType = CurrentAccountingPeriodView, isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()

  }
}
