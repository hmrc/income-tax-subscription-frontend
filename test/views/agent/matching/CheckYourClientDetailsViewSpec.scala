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

package views.agent.matching

import messagelookup.agent.MessageLookup
import messagelookup.agent.MessageLookup.{Base => common}
import models.DateModel
import models.usermatching.UserDetailsModel
import models.usermatching.UserDetailsModel._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.agent.TestConstants
import utilities.{TestModels, ViewSpec}
import views.agent.helpers.ConfirmClientIdConstants._
import views.html.agent.matching.CheckYourClientDetails

class CheckYourClientDetailsViewSpec extends ViewSpec {

  val testFirstName = "FirstName"
  val testLastName = "LastName"
  val testNino: String = "ZZ111111Z"
  val testDob: DateModel = TestModels.testStartDateThisYear
  val testClientDetails: UserDetailsModel = UserDetailsModel(
    testFirstName,
    testLastName,
    testNino,
    testDob)

  lazy val postAction: Call = controllers.agent.matching.routes.ConfirmClientController.submit()

  private val checkYourClientDetails = app.injector.instanceOf[CheckYourClientDetails]

  def page(): HtmlFormat.Appendable = checkYourClientDetails(
    userDetailsModel = testClientDetails,
    postAction = postAction
  )(FakeRequest(), implicitly)

  def document(): Document = Jsoup.parse(page().body)

  "YourClientDetailsCheckYourAnswers" must {
    "have the correct template" in new TemplateViewTest(
      view = page(),
      title = ConfirmClient.title,
      isAgent = true
    )

    "have a heading" in {
      document().getH1Element.text() mustBe ConfirmClient.heading
    }

    "have a caption" in {
      document().mainContent.selectHead(".govuk-caption-l").text() mustBe ConfirmClient.caption
    }

    "have a summary of answers" when {
      "display the correct info for client details" in {
        document().mainContent.mustHaveSummaryList(".govuk-summary-list")(Seq(
          SummaryListRowValues(
            key = ConfirmClient.firstName,
            value = Some("FirstName"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url,
                text = s"${ConfirmClient.change} ${ConfirmClient.firstName}",
                visuallyHidden = ConfirmClient.firstName
              )
            )
          ),
          SummaryListRowValues(
            key = ConfirmClient.lastName,
            value = Some("LastName"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url,
                text = s"${ConfirmClient.change} ${ConfirmClient.lastName}",
                visuallyHidden = ConfirmClient.lastName
              )
            )
          ),
          SummaryListRowValues(
            key = ConfirmClient.nino,
            value = Some("ZZ 11 11 11 Z"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url,
                text = s"${ConfirmClient.change} ${ConfirmClient.nino}",
                visuallyHidden = ConfirmClient.nino
              )
            )
          ),
          SummaryListRowValues(
            key = ConfirmClient.dob,
            value = Some("6 April 2024"),
            actions = Seq(
              SummaryListActionValues(
                href = controllers.agent.matching.routes.ClientDetailsController.show(editMode = true).url,
                text = s"${ConfirmClient.change} ${ConfirmClient.dob}",
                visuallyHidden = ConfirmClient.dob
              )
            )
          )
        ))
      }

      "have a form" which {
        def form: Element = document().mainContent.getForm

        "has the correct attributes" in {
          form.attr("method") mustBe "POST"
          form.attr("action") mustBe controllers.agent.matching.routes.ConfirmClientController.submit().url
        }

        "has a continue button" in {
          form.selectNth(".govuk-button", 1).text mustBe ConfirmClient.continue
        }
      }
    }

    object ConfirmClient {
      val title = "Check your answers - client’s details"
      val heading = "Check your answers"
      val caption = "This section is Details you are signing up your client with"
      val firstName = "First name"
      val lastName = "Last name"
      val nino = "National Insurance number"
      val dob = "Date of birth"
      val change = "Change"
      val continue = "Continue"
    }
  }
}
