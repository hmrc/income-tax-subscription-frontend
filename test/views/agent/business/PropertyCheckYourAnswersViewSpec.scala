/*
 * Copyright 2022 HM Revenue & Customs
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

import models.common.PropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import utilities.ViewSpec
import views.html.agent.business.PropertyCheckYourAnswers

class PropertyCheckYourAnswersViewSpec extends ViewSpec {
  private val propertyCheckYourAnswersView = app.injector.instanceOf[PropertyCheckYourAnswers]

  private val completeCashProperty = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val completeAccrualsProperty = PropertyModel(
    accountingMethod = Some(Accruals),
    startDate = Some(DateModel("8", "11", "2021")),
  )

  private val confirmedAccrualsProperty = completeAccrualsProperty.copy(
    confirmed = true
  )

  private val propertyWithMissingStartDate = PropertyModel(
    accountingMethod = Some(Cash)
  )

  private val propertyWithMissingAccountingMethod = PropertyModel(
    startDate = Some(DateModel("8", "11", "2021"))
  )

  private val incompleteProperty = PropertyModel()

  object PropertyCheckYourAnswers {
    val title = "Check your answers - UK property business"
    val heading = "Check your answers"
    val caption = "This section is UK property business you entered"
    val startDateQuestion = "UK property business trading start date"
    val accountMethodQuestion = "UK property business accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val add = "Add"
  }

  "Property CYA page" must {
    "have no error" in new TemplateViewTest(
      view = propertyCheckYourAnswersView(
        completeCashProperty,
        testCall,
        testBackUrl
      ),
      title = PropertyCheckYourAnswers.title,
      isAgent = true,
      backLink = Some(testBackUrl)
    )

    "have a heading" in {
      document()
        .select("h1")
        .text() mustBe PropertyCheckYourAnswers.heading
    }

    "have a caption" in {
      document()
        .select(".hmrc-page-heading p")
        .text() mustBe PropertyCheckYourAnswers.caption
    }

    "display property details" when {
      "all the answers have been completed" which {
        assertRow(
          document(viewModel = completeCashProperty),
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.agent.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          document(viewModel = completeCashProperty),
          section = "cash accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash basis accounting"),
          changeLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )
        assertRow(
          document(viewModel = completeAccrualsProperty),
          section = "accruals accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Traditional accounting"),
          changeLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )

        "have an enabled confirm and continue button" when {
          "all questions have been answered" in {
            val buttonLink: Element = document(viewModel = completeAccrualsProperty).selectHead(".govuk-button")
            buttonLink.text mustBe PropertyCheckYourAnswers.confirmedAndContinue
            buttonLink.hasAttr("disabled") mustBe false
          }
        }

        "have a save and come back later button" in {
          val buttonLink: Element = document(viewModel = completeAccrualsProperty).selectHead(".govuk-button--secondary")
          buttonLink.text mustBe PropertyCheckYourAnswers.saveAndComeBack
          buttonLink.attr("href") mustBe
            controllers.agent.business.routes.ProgressSavedController.show(Some("uk-property-check-your-answers")).url
        }


        "not have a save and come back later button if confirmed" in {
          val buttonLink: Option[Element] = document(viewModel = confirmedAccrualsProperty).selectOptionally(".govuk-button--secondary")
          buttonLink mustBe None
        }
      }


      "the start date is incomplete" which {
        val doc = document(viewModel = propertyWithMissingStartDate)

        assertRow(
          doc,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = None,
          changeLink = controllers.agent.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          doc,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = Some("Cash basis accounting"),
          changeLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )

        "have an enabled confirm and continue button" when {
          "the start day has not been answered" in {
            val buttonLink: Element = document(viewModel = propertyWithMissingStartDate).selectHead(".govuk-button")
            buttonLink.text mustBe PropertyCheckYourAnswers.confirmedAndContinue
            buttonLink.hasAttr("disabled") mustBe false
          }
        }
      }

      "the accounting method is incomplete" which {
        val doc = document(viewModel = propertyWithMissingAccountingMethod)

        assertRow(
          doc,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = Some("8 November 2021"),
          changeLink = controllers.agent.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          doc,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = None,
          changeLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )

        "have an enabled confirm and continue button" when {
          "the accounting method has not been answered" in {
            val buttonLink: Element = document(viewModel = propertyWithMissingAccountingMethod).selectHead(".govuk-button")
            buttonLink.text mustBe PropertyCheckYourAnswers.confirmedAndContinue
            buttonLink.hasAttr("disabled") mustBe false
          }
        }
      }

      "no answer has been completed" which {
        val doc = document(viewModel = incompleteProperty)

        assertRow(
          doc,
          section = "start date",
          index = 1,
          PropertyCheckYourAnswers.startDateQuestion,
          answer = None,
          changeLink = controllers.agent.business.routes.PropertyStartDateController.show(editMode = true).url
        )
        assertRow(
          doc,
          section = "accounting method",
          index = 2,
          PropertyCheckYourAnswers.accountMethodQuestion,
          answer = None,
          changeLink = controllers.agent.business.routes.PropertyAccountingMethodController.show(editMode = true).url
        )

        "have an enabled confirm and continue button" when {
          "the accounting method has not been answered" in {
            val buttonLink: Element = document(viewModel = incompleteProperty).selectHead(".govuk-button")
            buttonLink.text mustBe PropertyCheckYourAnswers.confirmedAndContinue
            buttonLink.hasAttr("disabled") mustBe false
          }
        }
      }
    }
  }

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

        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-summary-list__actions a span[aria-hidden=\"true\"]")
          .text() mustBe answer.fold(PropertyCheckYourAnswers.add)(_ => PropertyCheckYourAnswers.change)
      }

      s"display the section hidden content" in {
        doc.selectNth(".govuk-summary-list__row", index)
          .selectHead(".govuk-visually-hidden")
          .text() mustBe s"${answer.fold(PropertyCheckYourAnswers.add)(_ => PropertyCheckYourAnswers.change)} $question"
      }
    }
  }

  private def page(viewModel: PropertyModel) = propertyCheckYourAnswersView(
    viewModel,
    postAction = controllers.agent.business.routes.PropertyStartDateController.submit(),
    backUrl = "test-back-url"
  )

  private def document(viewModel: PropertyModel = completeCashProperty) = Jsoup.parse(page(viewModel).body)
}
