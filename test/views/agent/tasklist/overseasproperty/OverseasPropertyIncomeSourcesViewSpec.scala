/*
 * Copyright 2024 HM Revenue & Customs
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

package views.agent.tasklist.overseasproperty

import forms.agent.IncomeSourcesOverseasPropertyForm
import forms.submapping.YesNoMapping
import messagelookup.agent.MessageLookup.Base.{saveAndComeBackLater, saveAndContinue}
import models.{AccountingMethod, Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.overseasproperty.IncomeSourcesOverseasProperty


class OverseasPropertyIncomeSourcesViewSpec extends ViewSpec {

  object OverseasProppertyIncomeSourcesMessages {
    val title = "Your client’s foreign property"
    val heading: String = title
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val para1 = s"Did this income start before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}?"
    val para2 = "What accounting method does your client use for their foreign property business?"
    val radioCash = "Cash basis accounting"
    val radioAccruals = "Traditional accounting"
    val detailsSummary = "Help with accounting method"
    val detailsSubheading = "Example"
    val detailsContentPara = "Your client created an invoice for someone in March 2017, but did not receive the money until May 2017. If your client tells HMRC they received this income in:"
    val detailsBullet1 = "May 2017, they use cash basis accounting"
    val detailsBullet2 = "March 2017, you use traditional accounting"
    val maxDate = "The date the overseas property business started trading must be on or before 11 April 2021"
    val minDate = "The date your client’s property business started trading must be on or after 11 April 2021"
    val yes = "Yes"
    val no = "No"
  }

  val testErrorStartDate: FormError = FormError("startDate", "agent.error.property.day-month-year.empty")
  val testErrorAccountingMethod: FormError = FormError("accountingMethodProperty", "agent.error.accounting-method-property.invalid")

  val overseasPropertyIncomeSourcesForm: Form[(DateModel, AccountingMethod)] = IncomeSourcesOverseasPropertyForm.incomeSourcesOverseasPropertyForm(_.toString)
  val backUrl: String = testBackUrl
  val postAction: Call = testCall

  class Setup(form: Form[(DateModel, AccountingMethod)] = overseasPropertyIncomeSourcesForm) {
    val page: Html = app.injector.instanceOf[IncomeSourcesOverseasProperty].apply(
      form,
      postAction,
      backUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

    val document: Document = Jsoup.parse(page.body)
  }

  "UkPropertyIncomeSourcesView" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = app.injector.instanceOf[IncomeSourcesOverseasProperty].apply(
          overseasPropertyIncomeSourcesForm,
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasProppertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl)
      )

      "there is an error" in new TemplateViewTest(
        view = app.injector.instanceOf[IncomeSourcesOverseasProperty].apply(
          overseasPropertyIncomeSourcesForm.withError(testErrorStartDate).withError(testErrorAccountingMethod),
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = OverseasProppertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testErrorStartDate)
      )
    }

    "have a heading and caption" in new Setup {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = OverseasProppertyIncomeSourcesMessages.heading,
        caption = OverseasProppertyIncomeSourcesMessages.caption,
        isSection = false
      )
    }

    "have a form" which {

      "has the correct method and action" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "have a yes no radio input for the start date before limit field" in new Setup {
        document.mainContent.selectNth(".govuk-form-group", 1).mustHaveRadioInput(selector = ".govuk-form-group")(
          name = IncomeSourcesOverseasPropertyForm.startDateBeforeLimit,
          legend = OverseasProppertyIncomeSourcesMessages.para1,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(OverseasProppertyIncomeSourcesMessages.yes),
              value = Some(YesNoMapping.option_yes)
            ),
            RadioItem(
              content = Text(OverseasProppertyIncomeSourcesMessages.no),
              value = Some(YesNoMapping.option_no)
            )
          ),
          isInline = true
        )
      }

      "have a yes no radio input for accounting method" in new Setup {

        document.mainContent.selectNth(".govuk-form-group", 2).mustHaveRadioInput(selector = ".govuk-form-group")(
          name = IncomeSourcesOverseasPropertyForm.accountingMethodOverseasProperty,
          legend = OverseasProppertyIncomeSourcesMessages.para2,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(OverseasProppertyIncomeSourcesMessages.radioCash),
              value = Some(Cash.toString)
            ),
            RadioItem(
              content = Text(OverseasProppertyIncomeSourcesMessages.radioAccruals),
              value = Some(Accruals.toString)
            )
          )
        )
      }

      "has a save and continue + save and come back later buttons" in new Setup() {
        document.mainContent.selectHead(".govuk-button").text mustBe saveAndContinue
        document.mainContent.selectHead(".govuk-button--secondary").text mustBe saveAndComeBackLater
      }
    }

  }
}