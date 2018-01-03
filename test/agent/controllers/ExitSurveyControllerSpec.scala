/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import agent.assets.MessageLookup
import agent.audit.Logging
import agent.forms.ExitSurveyForm
import agent.models.ExitSurveyModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._


class ExitSurveyControllerSpec extends AgentControllerBaseSpec {

  override val controllerName: String = "ExitSurveyController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestExitSurveyController extends ExitSurveyController(
    app.injector.instanceOf[Logging],
    appConfig,
    messagesApi
  )

  val testSurvey = ExitSurveyModel("Very satisfied", "This is my extended feedback")

  "ExitSurveyController.show" should {
    val testOrigin = "/hello-world"
    lazy val result = TestExitSurveyController.show(testOrigin)(FakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"have the title '${MessageLookup.ExitSurvey.title}'" in {
      document.title() must be(MessageLookup.ExitSurvey.title)
    }
  }

  "ExitSurveyController.surveyFormDataToMap" should {

    def result(testData: ExitSurveyModel) = TestExitSurveyController.surveyFormDataToMap(testData)

    "generated the correct data map for the test survey" in {
      result(testSurvey) mustBe Map(
        ExitSurveyForm.satisfaction -> testSurvey.satisfaction.get,
        ExitSurveyForm.improvements -> testSurvey.improvements.get
      )
    }
    "generated the correct data map for the test survey when there is no satisfaction" in {
      result(testSurvey.copy(satisfaction = None)) mustBe Map(
        ExitSurveyForm.improvements -> testSurvey.improvements.get
      )
    }
    "generated the correct data map for the test survey when there is no improvements" in {
      result(testSurvey.copy(improvements = None)) mustBe Map(
        ExitSurveyForm.satisfaction -> testSurvey.satisfaction.get
      )
    }
    "generated the an empty map when nonthing is suplied" in {
      result(ExitSurveyModel(None, None)) mustBe Map()
    }
  }

  // N.B. currently the correctness of splunk cannot be unit tested due it being implemented as
  // a side effect async process call inside a unit return function
  // i.e. even if splunk fails these two tests would not fail
  // by separating the scenarios here we can at least manually examine the print outs for these respective tests
  // but the test for the splunk audit itself must be done manually in QA
  "ExitSurveyController.submit" when {

    "received an empty request" should {
      lazy val result = TestExitSurveyController.submit()(FakeRequest())

      "return SEE_OTHER (303)" in {
        status(result) must be(Status.SEE_OTHER)
      }

      s"redirect to '${agent.controllers.routes.ThankYouController.show().url}'" in {
        redirectLocation(result) mustBe Some(agent.controllers.routes.ThankYouController.show().url)
      }
    }

    "received an request with form data" should {

      val surveyData = TestExitSurveyController.surveyFormDataToMap(testSurvey)

      lazy val result = TestExitSurveyController.submit()(FakeRequest().post(ExitSurveyForm.exitSurveyValidationForm.fill(testSurvey)))

      "return SEE_OTHER (303)" in {
        status(result) must be(Status.SEE_OTHER)
      }

      s"redirect to '${agent.controllers.routes.ThankYouController.show().url}'" in {
        redirectLocation(result) mustBe Some(agent.controllers.routes.ThankYouController.show().url)
      }
    }
  }

}
