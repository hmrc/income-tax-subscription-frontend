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

import models.DateModel
import models.common.PropertyModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.tasklist.ukproperty.PropertyCheckYourAnswers

import java.time.format.DateTimeFormatter

class PropertyCheckYourAnswersViewSpec extends ViewSpec {

  "PropertyCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = propertyCheckYourAnswersView(
        viewModel = completeProperty,
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

    "display a summary of answers" when {
      "in edit mode" when {
        "data is complete" which {
          "has a start date" in {
            document(PropertyModel(startDate = Some(limitDate))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
              startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))))
            ))
          }
          "start date before limit field is present" which {
            "is true" in {
              document(PropertyModel(startDateBeforeLimit = Some(true))).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
                startDateRow(value = Some(PropertyCheckYourAnswers.beforeStartDateLimit))
              ))
            }
            "is false" when {
              "the stored start date is not older than the limit" in {
                document(PropertyModel(startDateBeforeLimit = Some(false), startDate = Some(limitDate))).mainContent
                  .mustHaveSummaryList(".govuk-summary-list")(Seq(
                    startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))))
                  ))
              }
              "the stored start date is older than the limit" in {
                document(PropertyModel(
                  startDateBeforeLimit = Some(false),
                  startDate = Some(olderThanLimitDate)
                )).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
                  startDateRow(value = Some(PropertyCheckYourAnswers.beforeStartDateLimit))
                ))
              }
            }
          }
        }
        "all data is missing" in {
          document(emptyProperty).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateRow(value = None)
          ))
        }
      }
      "in global edit mode" when {
        "all data is complete" in {
          document(completeProperty, isGlobalEdit = true).mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
            startDateRow(value = Some(limitDate.toLocalDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))), globalEditMode = true)
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

  private lazy val propertyCheckYourAnswersView = app.injector.instanceOf[PropertyCheckYourAnswers]
  private lazy val completeProperty: PropertyModel = PropertyModel(
    startDateBeforeLimit = Some(false),
    startDate = Some(limitDate)
  )
  private lazy val emptyProperty: PropertyModel = PropertyModel()
  private lazy val olderThanLimitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit.minusDays(1))
  private lazy val limitDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  object PropertyCheckYourAnswers {
    val title = "Check your answers - UK property"
    val heading = "Check your answers"
    val caption = "FirstName LastName | ZZ 11 11 11 Z"
    val startDateQuestion = "Property start date"
    val confirmedAndContinue = "Confirm and continue"
    val saveAndComeBack = "Save and come back later"
    val change = "Change"
    val add = "Add"
    val beforeStartDateLimit = s"Before 6 April ${AccountingPeriodUtil.getStartDateLimit.getYear}"
  }

  private def simpleSummaryRow(key: String): (Option[String], String) => SummaryListRowValues = {
    case (value, href) =>
      SummaryListRowValues(
        key = key,
        value = value,
        actions = Seq(
          SummaryListActionValues(
            href = href,
            text = (if (value.isDefined) PropertyCheckYourAnswers.change else PropertyCheckYourAnswers.add) + " " + key,
            visuallyHidden = key
          )
        )
      )
  }

  private def startDateRow(value: Option[String], globalEditMode: Boolean = false) = {
    simpleSummaryRow(PropertyCheckYourAnswers.startDateQuestion)(
      value,
      controllers.agent.tasklist.ukproperty.routes.PropertyStartDateBeforeLimitController.show(editMode = true, isGlobalEdit = globalEditMode).url
    )
  }

  private def page(viewModel: PropertyModel, isGlobalEdit: Boolean): HtmlFormat.Appendable = propertyCheckYourAnswersView(
    viewModel = viewModel,
    postAction = controllers.agent.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(isGlobalEdit = isGlobalEdit),
    isGlobalEdit = isGlobalEdit,
    backUrl = "test-back-url",
    clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")
  )

  private def document(viewModel: PropertyModel = completeProperty, isGlobalEdit: Boolean = false): Document = {
    Jsoup.parse(page(viewModel, isGlobalEdit).body)
  }

}

