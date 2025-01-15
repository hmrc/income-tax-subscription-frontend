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

package views.agent.tasklist.overseasproperty

import config.featureswitch.FeatureSwitch.AgentStreamline
import models.common.OverseasPropertyModel
import models.{Accruals, Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

class OverseasPropertyCheckYourAnswersViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentStreamline)
  }

  private val overseasPropertyCheckYourAnswersView = app.injector.instanceOf[OverseasPropertyCheckYourAnswers]

  object OverseasPropertyCheckYourAnswers {
    val title = "Check your answers - Foreign property"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val heading = "Check your answers"
    val startDate = "Start date"
    val addStartDate = "Add start date"
    val changeStartDate = "Change start date"
    val accountingMethod = "Accounting method"
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
    accountingMethod = Some(Cash)
  )

  private val completeAccrualsProperty = OverseasPropertyModel(
    startDate = Some(DateModel("8", "11", "2021")),
    accountingMethod = Some(Accruals)
  )

  private val incompleteProperty = OverseasPropertyModel()

  "OverseasPropertyCheckYourAnswers" must {
    "use the correct page template" in new TemplateViewTest(
      view = overseasPropertyCheckYourAnswersView(
        viewModel = completeCashProperty,
        postAction = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(),
        isGlobalEdit = false,
        backUrl = testBackUrl,
        clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
      ),
      title = OverseasPropertyCheckYourAnswers.title,
      isAgent = true,
      backLink = Some(testBackUrl)
    )

    "have a heading and caption" in {
      document(completeCashProperty).mainContent.mustHaveHeadingAndCaption(
        heading = OverseasPropertyCheckYourAnswers.heading,
        caption = OverseasPropertyCheckYourAnswers.caption,
        isSection = false
      )
    }

    "display a summary of answers" when {
      "the start date and cash accounting method is defined" in {
        document(completeCashProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDate,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDate}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDate
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountingMethod,
            value = Some("Cash basis accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountingMethod}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountingMethod
              )
            )
          )
        ))
      }

      "the start date and accruals accounting method is defined" in {
        document(completeAccrualsProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDate,
            value = Some("8 November 2021"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDate}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDate
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountingMethod,
            value = Some("Traditional accounting"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountingMethod}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountingMethod
              )
            )
          )
        ))
      }

      "when answers for the row are missing" in {
        document(incompleteProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.startDate,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.add} ${OverseasPropertyCheckYourAnswers.startDate}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.startDate
              )
            )
          ),
          SummaryListRowValues(
            key = OverseasPropertyCheckYourAnswers.accountingMethod,
            value = None,
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show(editMode = true).url,
                text = s"${OverseasPropertyCheckYourAnswers.add} ${OverseasPropertyCheckYourAnswers.accountingMethod}",
                visuallyHidden = OverseasPropertyCheckYourAnswers.accountingMethod
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
              key = OverseasPropertyCheckYourAnswers.startDate,
              value = Some("8 November 2021"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true).url,
                  text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDate}",
                  visuallyHidden = OverseasPropertyCheckYourAnswers.startDate
                )
              )
            ),
            SummaryListRowValues(
              key = OverseasPropertyCheckYourAnswers.accountingMethod,
              value = Some("Cash basis accounting"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true).url,
                  text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountingMethod}",
                  visuallyHidden = OverseasPropertyCheckYourAnswers.accountingMethod
                )
              )
            )
          ))
        }
        "in global edit mode" in {
          enable(AgentStreamline)

          document(completeCashProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            SummaryListRowValues(
              key = OverseasPropertyCheckYourAnswers.startDate,
              value = Some("8 November 2021"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true, isGlobalEdit = true).url,
                  text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.startDate}",
                  visuallyHidden = OverseasPropertyCheckYourAnswers.startDate
                )
              )
            ),
            SummaryListRowValues(
              key = OverseasPropertyCheckYourAnswers.accountingMethod,
              value = Some("Cash basis accounting"),
              actions = Seq(
                SummaryListActionValues(
                  href = controllers.agent.tasklist.overseasproperty.routes.IncomeSourcesOverseasPropertyController.show(editMode = true, isGlobalEdit = true).url,
                  text = s"${OverseasPropertyCheckYourAnswers.change} ${OverseasPropertyCheckYourAnswers.accountingMethod}",
                  visuallyHidden = OverseasPropertyCheckYourAnswers.accountingMethod
                )
              )
            )
          ))
        }
      }
    }

    "have a form" which {
      def form: Element = document(completeCashProperty).mainContent.getForm

      "has the correct attributes" in {
        form.attr("method") mustBe "POST"
        form.attr("action") mustBe controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit().url
      }

      "has a confirm and continue button" in {
        form.selectNth(".govuk-button", 1).text mustBe OverseasPropertyCheckYourAnswers.confirmedAndContinue
      }

      "has a save and comeback later button" in {
        val saveAndComeBackLater = form.selectNth(".govuk-button", 2)
        saveAndComeBackLater.text mustBe OverseasPropertyCheckYourAnswers.saveAndComeBack
        saveAndComeBackLater.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(location = Some("overseas-property-check-your-answers")).url
      }
    }
  }

  private def page(viewModel: OverseasPropertyModel, isGlobalEdit: Boolean) = overseasPropertyCheckYourAnswersView(
    viewModel = viewModel,
    postAction = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
    isGlobalEdit = isGlobalEdit,
    backUrl = "test-back-url",
    clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
  )

  private def document(viewModel: OverseasPropertyModel, isGlobalEdit: Boolean = false) = Jsoup.parse(page(viewModel, isGlobalEdit).body)
}
