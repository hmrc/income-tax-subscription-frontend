/*
 * Copyright 2023 HM Revenue & Customs
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

package views.agent.business

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.business.OverseasPropertyCheckYourAnswers

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {

  private val overseasPropertyCheckYourAnswersView = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val heading = "Check your answers"

    val startDate = "Start date"
    val addStartDate = "Add start date"
    val changeStartDate = "Change start date"

    val count = "Number of properties"
    val addCount = "Add number of properties"
    val changeCount = "Change number of properties"

    val accountingMethod = "Accounting method"
    val addAccountingMethod = "Add accounting method"
    val changeAccountingMethod = "Change accounting method"

    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val continue = "Continue"

    val change = "Change"
    val add = "Add"

    val cash = "Cash basis accounting"
    val accruals = "Traditional accounting"
  }

  private val completeCashProperty = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    count = Some(1),
    accountingMethod = Some(Cash)
  )

  private val completeAccrualsProperty = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    count = Some(1),
    accountingMethod = Some(Accruals)
  )

  private val propertyWithMissingStartDate = OverseasPropertyModel(
    count = Some(1),
    accountingMethod = Some(Cash)
  )

  private val propertyWithMissingAccountingMethod = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    count = Some(1)
  )

  private val propertyWithMissingCount = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    accountingMethod = Some(Accruals)
  )

  "Foreign Property CYA page" must {
    "use the correct page template" in new TemplateViewTest(
      view = overseasPropertyCheckYourAnswersView(
        completeCashProperty,
        testCall,
        testBackUrl,
        ClientDetails("FirstName LastName", "ZZ111111Z")
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      isAgent = true,
      backLink = Some(testBackUrl),
    )

    "have a heading" in {
      document(viewModel = completeCashProperty)
        .selectHead("h1.govuk-heading-xl")
        .text() mustBe OverseasPropertyCheckYourAnswers.heading
    }

    "have a caption on the heading" in {
      document(viewModel = completeCashProperty)
        .selectHead("span.govuk-caption-xl")
        .text mustBe OverseasPropertyCheckYourAnswers.caption
    }

    "display the overseas property start date row" when {
      "the start date is complete" in {
        val row = document(viewModel = completeCashProperty)
          .selectNth(".govuk-summary-list__row", 1)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.startDate
        row.selectHead(".govuk-summary-list__value").text mustBe "8 November 2021"

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyStartDateController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeStartDate
      }
      "the start date is incomplete" in {
        val row = document(viewModel = propertyWithMissingStartDate)
          .selectNth(".govuk-summary-list__row", 1)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.startDate
        row.selectHead(".govuk-summary-list__value").text mustBe ""

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyStartDateController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addStartDate
      }
    }

    "display the overseas property accounting method row" when {
      "the accounting method is complete with cash" in {
        val row = document(viewModel = completeCashProperty)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountingMethod
        row.selectHead(".govuk-summary-list__value").text mustBe OverseasPropertyCheckYourAnswers.cash

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeAccountingMethod
      }
      "the accounting method is complete with accruals" in {
        val row = document(viewModel = completeAccrualsProperty)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountingMethod
        row.selectHead(".govuk-summary-list__value").text mustBe OverseasPropertyCheckYourAnswers.accruals

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeAccountingMethod
      }
      "the accounting method is incomplete" in {
        val row = document(viewModel = propertyWithMissingAccountingMethod)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountingMethod
        row.selectHead(".govuk-summary-list__value").text mustBe ""

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addAccountingMethod
      }
    }

    "have a confirm and continue button" when {
      "the foreign property business has not been confirmed" in {
        document(viewModel = completeCashProperty)
          .mainContent
          .selectHead("div.govuk-button-group")
          .selectHead("button").text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
      }
    }

    "have the save and come back later button" when {
      "the foreign property business has not been confirmed" in {
        val buttonLink: Element = document(viewModel = completeCashProperty)
          .mainContent
          .selectHead(".govuk-button--secondary")
        buttonLink.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
        buttonLink.attr("href") mustBe
          controllers.agent.business.routes.ProgressSavedController.show().url + "?location=overseas-property-check-your-answers"
      }
    }

    "have a continue button" when {
      "the foreign property business has been confirmed" in {
        document(viewModel = completeCashProperty.copy(confirmed = true))
          .mainContent
          .selectHead("button")
          .text mustBe OverseasPropertyCheckYourAnswers.continue
      }
    }
  }

  "Overseas Property CYA page" when {
    "the task list redesign feature switch is enabled" should {
      "display the overseas property count row" when {
        "the count is complete" in {
          enable(EnableTaskListRedesign)

          val row = document(viewModel = completeCashProperty)
            .selectNth(".govuk-summary-list__row", 2)

          row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.count
          row.selectHead(".govuk-summary-list__value").text mustBe "1"

          val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

          link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCountController.show(true).url
          link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
          link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeCount
        }
        "the count is incomplete" in {
          enable(EnableTaskListRedesign)

          val row = document(viewModel = propertyWithMissingCount)
            .selectNth(".govuk-summary-list__row", 2)

          row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.count
          row.selectHead(".govuk-summary-list__value").text mustBe ""

          val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

          link.attr("href") mustBe controllers.agent.business.routes.OverseasPropertyCountController.show(true).url
          link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
          link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addCount
        }
      }
    }
  }

  private def page(viewModel: OverseasPropertyModel) = overseasPropertyCheckYourAnswersView(
    viewModel,
    postAction = controllers.agent.business.routes.OverseasPropertyStartDateController.submit(),
    backUrl = "test-back-url",
    ClientDetails("FirstName LastName", "ZZ111111Z")
  )

  private def document(viewModel: OverseasPropertyModel) = Jsoup.parse(page(viewModel).body)
}
