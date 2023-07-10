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

package views.individual.incometax.business

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.ViewSpec
import views.html.individual.incometax.business.OverseasPropertyCheckYourAnswers

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    disable(EnableTaskListRedesign)
    super.beforeEach()
  }

  private val view = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val heading = "Check your answers"
    val caption = "This section is Foreign property"
    val startDateQuestion = "Start date"
    val countQuestion = "Number of properties"
    val accountMethodQuestion = "Accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val changeStartDate = "Change start date"
    val changeCount = "Change number of properties"
    val changeAccountingMethod = "Change accounting method"
    val add = "Add"
    val addStartDate = "Add start date"
    val addCount = "Add number of properties"
    val addAccountingMethod = "Add accounting method"
    val cash = "Cash basis accounting"
    val accruals = "Traditional accounting"
  }

  private val completeCashProperty = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    count = Some(1),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val completeAccrualsProperty = OverseasPropertyModel(
    accountingMethod = Some(Accruals),
    count = Some(1),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val propertyWithMissingStartDate = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    count = Some(1),
  )

  private val propertyWithMissingAccountingMethod = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    count = Some(1),
  )

  private val propertyWithMissingCount = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    accountingMethod = Some(Cash)
  )

  "Overseas Property CYA page" must {
    "use the correct template" in new TemplateViewTest(
      view = view(
        completeCashProperty,
        testCall,
        testBackUrl
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      backLink = Some(testBackUrl)
    )

    "have a heading" in {
      document(viewModel = completeCashProperty)
        .getH1Element
        .text() mustBe OverseasPropertyCheckYourAnswers.heading
    }

    "have a caption on the heading" in {
      document(viewModel = completeCashProperty)
        .selectHead(".hmrc-page-heading p")
        .text mustBe OverseasPropertyCheckYourAnswers.caption
    }

    "display the overseas property start date row" when {
      "the start date is complete" in {
        val row = document(viewModel = completeCashProperty)
          .selectNth(".govuk-summary-list__row", 1)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.startDateQuestion
        row.selectHead(".govuk-summary-list__value").text mustBe "8 November 2021"

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeStartDate
      }
      "the start date is incomplete" in {
        val row = document(viewModel = propertyWithMissingStartDate)
          .selectNth(".govuk-summary-list__row", 1)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.startDateQuestion
        row.selectHead(".govuk-summary-list__value").text mustBe ""

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addStartDate
      }
    }

    "display the overseas property accounting method row" when {
      "the accounting method is complete with cash" in {
        val row = document(viewModel = completeCashProperty)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountMethodQuestion
        row.selectHead(".govuk-summary-list__value").text mustBe OverseasPropertyCheckYourAnswers.cash

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeAccountingMethod
      }
      "the accounting method is complete with accruals" in {
        val row = document(viewModel = completeAccrualsProperty)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountMethodQuestion
        row.selectHead(".govuk-summary-list__value").text mustBe OverseasPropertyCheckYourAnswers.accruals

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeAccountingMethod
      }
      "the accounting method is incomplete" in {
        val row = document(viewModel = propertyWithMissingAccountingMethod)
          .selectNth(".govuk-summary-list__row", 2)

        row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.accountMethodQuestion
        row.selectHead(".govuk-summary-list__value").text mustBe ""

        val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

        link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(true).url
        link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
        link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addAccountingMethod
      }
    }

    "have a confirm and continue button" in {
      document(viewModel = completeCashProperty)
        .mainContent
        .selectHead("div.govuk-button-group")
        .selectHead("button").text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
    }

    "have the save and come back later button" in {
      val buttonLink: Element = document(viewModel = completeCashProperty)
        .mainContent
        .selectHead(".govuk-button--secondary")
      buttonLink.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
      buttonLink.attr("href") mustBe
        controllers.individual.business.routes.ProgressSavedController.show().url + "?location=overseas-property-check-your-answers"
    }
  }

  "Overseas Property CYA page" when {
    "the task list redesign feature switch is enabled" should {
      "display the overseas property count row" when {
        "the count is complete" in {
          enable(EnableTaskListRedesign)

          val row = document(viewModel = completeCashProperty)
            .selectNth(".govuk-summary-list__row", 2)

          row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.countQuestion
          row.selectHead(".govuk-summary-list__value").text mustBe "1"

          val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

          link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyCountController.show(true).url
          link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.change
          link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.changeCount
        }
        "the count is incomplete" in {
          enable(EnableTaskListRedesign)

          val row = document(viewModel = propertyWithMissingCount)
            .selectNth(".govuk-summary-list__row", 2)

          row.selectHead(".govuk-summary-list__key").text mustBe OverseasPropertyCheckYourAnswers.countQuestion
          row.selectHead(".govuk-summary-list__value").text mustBe ""

          val link = row.selectHead(".govuk-summary-list__actions").selectHead("a")

          link.attr("href") mustBe controllers.individual.business.routes.OverseasPropertyCountController.show(true).url
          link.selectHead("span[aria-hidden=\"true\"]").text mustBe OverseasPropertyCheckYourAnswers.add
          link.selectHead("span.govuk-visually-hidden").text mustBe OverseasPropertyCheckYourAnswers.addCount
        }
      }
    }

  }

  private def page(viewModel: OverseasPropertyModel) = view(
    viewModel,
    postAction = controllers.individual.business.routes.OverseasPropertyStartDateController.submit(),
    backUrl = "test-back-url"
  )

  private def document(viewModel: OverseasPropertyModel) = Jsoup.parse(page(viewModel).body)
}
