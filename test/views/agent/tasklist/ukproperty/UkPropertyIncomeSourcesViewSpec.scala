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

package views.agent.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import forms.agent.UkPropertyIncomeSourcesForm
import forms.submapping.YesNoMapping
import messagelookup.agent.MessageLookup.Base.{saveAndComeBackLater, saveAndContinue}
import models.{AccountingMethod, Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.ukproperty.PropertyIncomeSources

class UkPropertyIncomeSourcesViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(StartDateBeforeLimit)
  }

  "UkPropertyIncomeSourcesView" must {

    "have the correct page template" when {
      "there is no error" in new TemplateViewTest(
        view = propertyIncomeSources.apply(
          propertyIncomeSourcesForm,
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = UkPropertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl)
      )

      "there is an error" in new TemplateViewTest(
        view = propertyIncomeSources.apply(
          propertyIncomeSourcesForm.withError(testErrorStartDate).withError(testErrorAccountingMethod),
          testCall,
          testBackUrl,
          ClientDetails("FirstName LastName", "ZZ111111Z")
        ),
        title = UkPropertyIncomeSourcesMessages.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        error = Some(testErrorStartDate)
      )
    }

    "have a heading and caption" in new Setup {
      document.mainContent.mustHaveHeadingAndCaption(
        heading = UkPropertyIncomeSourcesMessages.heading,
        caption = UkPropertyIncomeSourcesMessages.caption,
        isSection = false
      )
    }

    "have a form" which {

      "has the correct method and action" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "if the start date before limit feature switch is disabled" should {
        "have a date fieldset" in new Setup {
          document.mustHaveDateInput(
            "startDate",
            UkPropertyIncomeSourcesMessages.para1,
            UkPropertyIncomeSourcesMessages.dateHint,
            isHeading = false,
            isLegendHidden = false,
            dateInputsValues = Seq(
              DateInputFieldValues("Day", None),
              DateInputFieldValues("Month", None),
              DateInputFieldValues("Year", None)
            ))
        }

        "have the correct radio inputs" in new Setup {
          document.mustHaveRadioInput(selector = ".govuk-form-group:nth-of-type(2)")(
            name = UkPropertyIncomeSourcesForm.accountingMethodProperty,
            legend = UkPropertyIncomeSourcesMessages.para2,
            isHeading = false,
            isLegendHidden = true,
            hint = None,
            errorMessage = None,
            radioContents = Seq(
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.radioCash),
                value = Some(Cash.toString)
              ),
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.radioAccruals),
                value = Some(Accruals.toString)
              )
            )
          )
        }
      }

      "if the start date before limit feature switch is enabled" should {
        "have a yes no radio input for the start date before limit field" in new Setup {
          enable(StartDateBeforeLimit)

          document.mainContent.selectNth(".govuk-form-group", 1).mustHaveRadioInput(selector = ".govuk-form-group")(
            name = UkPropertyIncomeSourcesForm.startDateBeforeLimit,
            legend = UkPropertyIncomeSourcesMessages.dateBeforeLimitLegend,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            radioContents = Seq(
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.yes),
                value = Some(YesNoMapping.option_yes)
              ),
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.no),
                value = Some(YesNoMapping.option_no)
              )
            ),
            isInline = true
          )
        }

        "have the correct radio inputs" in new Setup {
          enable(StartDateBeforeLimit)

          document.mainContent.selectNth(".govuk-form-group", 2).mustHaveRadioInput(selector = ".govuk-form-group")(
            name = UkPropertyIncomeSourcesForm.accountingMethodProperty,
            legend = UkPropertyIncomeSourcesMessages.para2,
            isHeading = false,
            isLegendHidden = true,
            hint = None,
            errorMessage = None,
            radioContents = Seq(
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.radioCash),
                value = Some(Cash.toString)
              ),
              RadioItem(
                content = Text(UkPropertyIncomeSourcesMessages.radioAccruals),
                value = Some(Accruals.toString)
              )
            )
          )
        }
      }

      "has a save and continue + save and come back later buttons" in new Setup() {
        document.mainContent.selectHead(".govuk-button").text mustBe saveAndContinue
        document.mainContent.selectHead(".govuk-button--secondary").text mustBe saveAndComeBackLater
      }
    }

    "must display max date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.max-date", List("11 April 2021"))
      val formWithError = propertyIncomeSourcesForm.withError(dateValidationError)

      val doc = new Setup(form = formWithError).document

      doc.mustHaveDateInput(
        id = "startDate",
        legend = UkPropertyIncomeSourcesMessages.para1,
        exampleDate = UkPropertyIncomeSourcesMessages.dateHint,
        errorMessage = Some(UkPropertyIncomeSourcesMessages.maxDate),
        isHeading = false,
        isLegendHidden = false,
        dateInputsValues = Seq(
          DateInputFieldValues("Day", None),
          DateInputFieldValues("Month", None),
          DateInputFieldValues("Year", None)
        )
      )
    }

    "must display min date error form error on page" in {
      val dateValidationError = FormError("startDate", "agent.error.property.day-month-year.min-date", List("11 April 2021"))
      val formWithError = propertyIncomeSourcesForm.withError(dateValidationError)

      val doc = new Setup(form = formWithError).document

      doc.mustHaveDateInput(
        id = "startDate",
        legend = UkPropertyIncomeSourcesMessages.para1,
        exampleDate = UkPropertyIncomeSourcesMessages.dateHint,
        errorMessage = Some(UkPropertyIncomeSourcesMessages.minDate),
        isHeading = false,
        isLegendHidden = false,
        dateInputsValues = Seq(
          DateInputFieldValues("Day", None),
          DateInputFieldValues("Month", None),
          DateInputFieldValues("Year", None)
        )
      )
    }
  }

  object UkPropertyIncomeSourcesMessages {
    val title = "Your client’s UK property"
    val heading: String = title
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val dateBeforeLimitLegend = s"Did this income start before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}?"
    val para1 = "When did your client’s UK property business start?"
    val dateHint = "For example, 17 8 2014."
    val para2 = "What accounting method does your client use for their UK property business?"
    val radioCash = "Cash basis accounting"
    val radioAccruals = "Traditional accounting"
    val detailsSummary = "Help with accounting method"
    val detailsSubheading = "Example"
    val detailsContentPara = "Your client created an invoice for someone in March 2017, but did not receive the money until May 2017. If your client tells HMRC they received this income in:"
    val detailsBullet1 = "May 2017, they use cash basis accounting"
    val detailsBullet2 = "March 2017, you use traditional accounting"
    val maxDate = "The date cannot be more than 7 days in the future"
    val minDate = "The date must be on or after 11 April 2021"
    val yes = "Yes"
    val no = "No"
  }

  lazy val testErrorStartDate: FormError = FormError("startDate", "agent.error.property.day-month-year.empty")
  lazy val testErrorAccountingMethod: FormError = FormError("accountingMethodProperty", "agent.error.accounting-method-property.invalid")

  lazy val propertyIncomeSourcesForm: Form[(DateModel, AccountingMethod)] = UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm(_.toString)

  lazy val propertyIncomeSources: PropertyIncomeSources = app.injector.instanceOf[PropertyIncomeSources]

  class Setup(form: Form[(DateModel, AccountingMethod)] = propertyIncomeSourcesForm) {
    def page: Html = propertyIncomeSources(
      form,
      testCall,
      testBackUrl,
      ClientDetails("FirstName LastName", "ZZ111111Z")
    )(FakeRequest(), implicitly)

    def document: Document = Jsoup.parse(page.body)
  }

}

