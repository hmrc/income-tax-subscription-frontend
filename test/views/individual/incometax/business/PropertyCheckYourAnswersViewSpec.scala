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

import models.common.PropertyModel
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.ViewSpec
import views.html.individual.incometax.business.PropertyCheckYourAnswers

class PropertyCheckYourAnswersViewSpec extends ViewSpec {

  private val propertyCheckYourAnswersView = app.injector.instanceOf[PropertyCheckYourAnswers]

  private val completeCashProperty = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val completeAccrualsProperty = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val propertyWithMissingStartDate = PropertyModel(
    accountingMethod = Some(Cash)
  )

  private val propertyWithMissingAccountingMethod = PropertyModel(
    startDate = Some(DateModel("8", "11", "2021"))
  )

  private val incompleteProperty = PropertyModel()

  object PropertyCheckYourAnswers {
    val title = "Check your answers"
    val heading = "Check your answers"
    val startDateQuestion = "UK property business trading start date"
    val accountMethodQuestion = "UK property business accounting method"
    val continue = "Continue"
    val change = "Change"
    val incomplete = "Incomplete"
  }

  "Property CYA page" must {
    "have no error" in new TemplateViewTest(
      view = propertyCheckYourAnswersView(
        completeCashProperty,
        testCall,
        testBackUrl
      ),
      title = PropertyCheckYourAnswers.title,
      backLink = Some(testBackUrl),
      hasSignOutLink = true
    )

    "have a heading" in {
      document()
        .select("h1")
        .text() mustBe PropertyCheckYourAnswers.heading
    }

    "have a continue button" in {
      val buttonLink: Element = document().selectHead(".govuk-button")
      buttonLink.text mustBe PropertyCheckYourAnswers.continue
    }


    "display property details" when {
      "all the answers have been completed" which {
        assertRow(
          viewModel = completeCashProperty,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.individual.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          viewModel = completeCashProperty,
          section = "cash accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash accounting"),
          changeLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
        assertRow(
          viewModel = completeAccrualsProperty,
          section = "accruals accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash accounting"),
          changeLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
      }

      "the start date is incomplete" which {
        assertRow(
          viewModel = propertyWithMissingStartDate,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          viewModel = propertyWithMissingStartDate,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash accounting"),
          changeLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
      }

      "the accounting method is incomplete" which {
        assertRow(
          viewModel = propertyWithMissingAccountingMethod,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.individual.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          viewModel = propertyWithMissingAccountingMethod,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
      }

      "no answer has been completed" which {
        assertRow(
          viewModel = incompleteProperty,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          viewModel = incompleteProperty,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = None,
          changeLink = controllers.individual.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
      }
    }
  }

  private def assertRow(viewModel: PropertyModel, section: String, index: Int, question: String, answer: Option[String], changeLink: String): Unit = {
    s"contains the $section row" that {
      "display the section question" in {
        document(viewModel).selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__key")
          .text() mustBe question
      }

      s"display the section answer" in {
        document(viewModel).selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__value")
          .text() mustBe answer.getOrElse("")
      }

      s"display the section edit link" in {
        document(viewModel).selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__actions a")
          .attr("href") mustBe changeLink
      }

      s"display the section hidden content" in {
        document(viewModel).selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-link")
          .text() mustBe s"${answer.fold(PropertyCheckYourAnswers.incomplete)(_ => PropertyCheckYourAnswers.change)} $question"
      }
    }
  }

  private def page(viewModel: PropertyModel) = propertyCheckYourAnswersView(
    viewModel,
    postAction = controllers.individual.business.routes.PropertyStartDateController.submit(),
    backUrl = "test-back-url"
  )

  private def document(viewModel: PropertyModel = completeCashProperty) = Jsoup.parse(page(viewModel).body)
}
