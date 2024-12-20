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

package views.agent.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.AgentStreamline
import models.common.PropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.ukproperty.PropertyCheckYourAnswers

class PropertyCheckYourAnswersViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentStreamline)
  }

  private val propertyCheckYourAnswersView = app.injector.instanceOf[PropertyCheckYourAnswers]

  private val completeCashProperty = PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("8", "11", "2021")))
  private val completeAccrualsProperty = completeCashProperty.copy(accountingMethod = Some(Accruals))
  private val incompleteProperty = PropertyModel()

  object PropertyCheckYourAnswers {
    val title = "Check your answers - UK property"
    val heading = "Check your answers"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val startDateQuestion = "Start date"
    val accountMethodQuestion = "Accounting method"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val add = "Add"
  }

  "PropertyCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = propertyCheckYourAnswersView(
        viewModel = completeCashProperty,
        postAction = testCall,
        isGlobalEdit = false,
        backUrl = testBackUrl,
        clientDetails = ClientDetails("", "")
      ),
      title = PropertyCheckYourAnswers.title,
      isAgent = true,
      backLink = Some(testBackUrl)
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = PropertyCheckYourAnswers.heading,
        caption = PropertyCheckYourAnswers.caption,
        isSection = false
      )
    }

    "have summary of answers" when {
      "start date and cash accounting method are defined" in {
        document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.startDateQuestion,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.accountMethodQuestion,
            value = Some("Cash basis accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyAccountingMethodController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }

      "start date and accruals accounting method are defined" in {
        document(completeAccrualsProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.startDateQuestion,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.accountMethodQuestion,
            value = Some("Traditional accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyAccountingMethodController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }


      "all answers are missing" in {
        document(incompleteProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.startDateQuestion,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.add} ${PropertyCheckYourAnswers.startDateQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.startDateQuestion
              )
            )
          ),
          SummaryListRowValues(
            key = PropertyCheckYourAnswers.accountMethodQuestion,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.ukproperty.routes.PropertyAccountingMethodController.show(editMode = true).url,
                text = s"${PropertyCheckYourAnswers.add} ${PropertyCheckYourAnswers.accountMethodQuestion}",
                visuallyHidden = PropertyCheckYourAnswers.accountMethodQuestion
              )
            )
          )
        ))
      }

      "when the agent streamline feature switch is enabled" when {
        "not in edit mode" in {
          enable(AgentStreamline)

          document(completeCashProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = PropertyCheckYourAnswers.startDateQuestion,
              value = Some("8 November 2021"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.ukproperty.routes.PropertyIncomeSourcesController.show(editMode = true).url,
                  text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.startDateQuestion}",
                  visuallyHidden = PropertyCheckYourAnswers.startDateQuestion
                )
              )
            ),
            SummaryListRowValues(
              key = PropertyCheckYourAnswers.accountMethodQuestion,
              value = Some("Cash basis accounting"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.ukproperty.routes.PropertyIncomeSourcesController.show(editMode = true).url,
                  text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.accountMethodQuestion}",
                  visuallyHidden = PropertyCheckYourAnswers.accountMethodQuestion
                )
              )
            )
          ))
        }
        "in global edit mode" in {
          enable(AgentStreamline)

          document(completeCashProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = PropertyCheckYourAnswers.startDateQuestion,
              value = Some("8 November 2021"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.ukproperty.routes.PropertyIncomeSourcesController.show(editMode = true, isGlobalEdit = true).url,
                  text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.startDateQuestion}",
                  visuallyHidden = PropertyCheckYourAnswers.startDateQuestion
                )
              )
            ),
            SummaryListRowValues(
              key = PropertyCheckYourAnswers.accountMethodQuestion,
              value = Some("Cash basis accounting"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.ukproperty.routes.PropertyIncomeSourcesController.show(editMode = true, isGlobalEdit = true).url,
                  text = s"${PropertyCheckYourAnswers.change} ${PropertyCheckYourAnswers.accountMethodQuestion}",
                  visuallyHidden = PropertyCheckYourAnswers.accountMethodQuestion
                )
              )
            )
          ))
        }
      }
    }

    "have a form" which {
      def form: Element = document().mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit().url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe PropertyCheckYourAnswers.confirmedAndContinue
      }

      "has a save and come back later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe PropertyCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("uk-property-check-your-answers")).url
      }
    }

  }

  private def page(viewModel: PropertyModel, isGlobalEdit: Boolean = false) = propertyCheckYourAnswersView(
    viewModel = viewModel,
    postAction = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
    isGlobalEdit = isGlobalEdit,
    backUrl = "test-back-url",
    clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
  )

  private def document(viewModel: PropertyModel = completeCashProperty, isGlobalEdit: Boolean = false) = Jsoup.parse(page(viewModel, isGlobalEdit).body)
}

