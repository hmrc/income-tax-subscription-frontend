/*
 * Copyright 2021 HM Revenue & Customs
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

import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import utilities.ViewSpec
import views.html.individual.incometax.business.OverseasPropertyCheckYourAnswers

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers"
    val heading = "Check your answers"
    val startDateQuestion = "Overseas property business trading start date"
    val accountMethodQuestion = "Overseas property business accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val continue = "Continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val add = "Add"
  }

  private val confirmedProperty = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021")),
    confirmed = true
  )

  private val completeCashProperty = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021"))
  )

  private val completeAccrualsProperty = OverseasPropertyModel(
    accountingMethod = Some(Accruals),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val propertyWithMissingStartDate = OverseasPropertyModel(
    accountingMethod = Some(Cash)
  )

  private val propertyWithMissingAccountingMethod = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021"))
  )

  "Overseas Property CYA page" must {
    "have no error" in new TemplateViewTest(
      view = view(
        completeCashProperty,
        testCall,
        testBackUrl
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading" in {
      document(viewModel = completeCashProperty)
        .select("h1")
        .text() mustBe OverseasPropertyCheckYourAnswers.heading
    }

    "have overseas property details" when {
      "all the answers have been completed" which {
        assertRow(
          document(viewModel = completeCashProperty),
          section = "start date",
          index = 1,
          OverseasPropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.individual.business.routes.OverseasPropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          document(viewModel = completeCashProperty),
          section = "cash accounting method",
          index = 2,
          OverseasPropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash accounting"),
          changeLink = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
        )
        assertRow(
          document(viewModel = completeAccrualsProperty),
          section = "accruals accounting method",
          index = 2,
          OverseasPropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Standard accounting"),
          changeLink = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
        )
      }

      "the start date is incomplete" which {
        val doc = document(viewModel = propertyWithMissingStartDate)

        assertRow(
          doc,
          section = "start date",
          index = 1,
          OverseasPropertyCheckYourAnswers.startDateQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.OverseasPropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          doc,
          section = "accounting method",
          index = 2,
          OverseasPropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash accounting"),
          changeLink = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
        )
      }

      "the accounting method is incomplete" which {
        val doc = document(viewModel = propertyWithMissingAccountingMethod)

        assertRow(
          doc,
          section = "start date",
          index = 1,
          OverseasPropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.individual.business.routes.OverseasPropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          doc,
          section = "accounting method",
          index = 2,
          OverseasPropertyCheckYourAnswers.accountMethodQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url
        )
      }
    }

    "have the confirm and continue button" when {
      "the answers haven't been confirmed" in {
        val buttonLink: Element = document(viewModel = completeCashProperty).selectHead(".govuk-button")
        buttonLink.text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
        buttonLink.hasAttr("disabled") mustBe false
      }
    }

    "disable the confirm and continue button" when {
      "not all questions have been answered" in {
        val buttonLink: Element = document(viewModel = propertyWithMissingStartDate).selectHead(".govuk-button")
        buttonLink.text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
        buttonLink.hasAttr("disabled") mustBe true
      }
    }

    "have the continue button" when {
      "the answers have been confirmed" in {
        val buttonLink: Element = document(viewModel = confirmedProperty).selectHead(".govuk-button")
        buttonLink.text mustBe OverseasPropertyCheckYourAnswers.continue
      }
    }

    "have the save and come back later button" in {
      val buttonLink: Element = document(viewModel = completeCashProperty).selectHead(".govuk-button--secondary")
      buttonLink.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
      buttonLink.attr("href") mustBe controllers.individual.business.routes.ProgressSavedController.show().url
    }
  }

  private def page(viewModel: OverseasPropertyModel) = view(
    viewModel,
    postAction = controllers.individual.business.routes.OverseasPropertyStartDateController.submit(),
    backUrl = "test-back-url"
  )

  private def assertRow(
                         doc: Document,
                         section: String,
                         index: Int, question: String,
                         answer: Option[String],
                         changeLink: String
                       ): Unit = {
    s"contains the $section row" that {
      "display the section question" in {
        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__key")
          .text() mustBe question
      }

      s"display the section answer" in {
        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__value")
          .text() mustBe answer.getOrElse("")
      }

      s"display the section edit link" in {
        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__actions a")
          .attr("href") mustBe changeLink
      }

      s"display the section hidden content" in {
        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-visually-hidden")
          .text() mustBe s"${answer.fold(OverseasPropertyCheckYourAnswers.add)(_ => OverseasPropertyCheckYourAnswers.change)} $question"
      }
    }
  }

  private def document(viewModel: OverseasPropertyModel) = Jsoup.parse(page(viewModel).body)
}