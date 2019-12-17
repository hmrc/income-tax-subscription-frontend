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
import core.views.ViewSpecTrait
import incometax.util.AccountingPeriodUtil
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.twirl.api.Html

class BusinessAccountingPeriodDateViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall
  val taxEndYear = AccountingPeriodUtil.getCurrentTaxYear.taxEndYear

  def page(isEditMode: Boolean, addFormErrors: Boolean): Html = agent.views.html.business.accounting_period_date(
    accountingPeriodForm = AccountingPeriodDateForm.accountingPeriodDateForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode = isEditMode,
    taxEndYear = taxEndYear
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(suffix: Option[String] = None, isEditMode: Boolean): TestView = TestView(
    name = s"Business Accounting Period Date View${suffix.fold("")(x => x)}",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, addFormErrors = false)
  )

  "The Business Accounting Period Date view" should {

    val testPage = documentCore(
      isEditMode = false
    )

    testPage.mustHaveBackLinkTo(backUrl)

    val form = testPage.getForm(s"Business Accounting Period Date form")(actionCall = action)

    form.mustHaveDateField(
      id = "startDate",
      legend = common.startDate,
      exampleDate = messages.exampleStartDate(taxEndYear - 1)
    )

    form.mustHaveDateField(
      id = "endDate",
      legend = common.endDate,
      exampleDate = messages.exampleEndDate(taxEndYear)
    )

    val editModePage = documentCore(
      suffix = " and it is in edit mode",
      isEditMode = true
    )

    editModePage.mustHavePara(messages.line1)

    editModePage.mustHaveUpdateButton()

  }

  "Append Error to the page title if the form has error" should {

    def documentCore() = TestView(
      name = s"Business Accounting Period Date View",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, addFormErrors = true)
    )

    val testPage = documentCore()

  }
}
