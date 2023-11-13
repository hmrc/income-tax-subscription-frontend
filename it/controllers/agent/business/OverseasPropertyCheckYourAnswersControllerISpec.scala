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

package controllers.agent.business

import _root_.common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.subscriptionUri
import helpers.IntegrationTestConstants.{testFirstName, testLastName}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.{taskListURI, testNino, testUtr, yourIncomeSourcesURI}
import helpers.agent.WiremockHelper.verifyPost
import helpers.agent.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty
import utilities.UserMatchingSessionUtil

class OverseasPropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EnableTaskListRedesign)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))

      When("GET business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers(
        Map(
          ITSASessionKeys.UTR -> testUtr,
          UserMatchingSessionUtil.firstName -> testFirstName,
          UserMatchingSessionUtil.lastName -> testLastName,
          ITSASessionKeys.NINO -> testNino
        )
      )

      Then("Should return OK with the property CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.overseas-property.check-your-answers.title")} - Use software to report your client’s Income Tax - GOV.UK"
        )
      )
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-check-your-answers" should {
    "redirect to the agent your income source page if the task list redesign feature switch is enabled" when {
      "the user answered all the overseas property questions" should {
        "save the property answers" in {
          enable(EnableTaskListRedesign)

          val testProperty = OverseasPropertyModel(
            accountingMethod = Some(Cash),
            startDate = Some(DateModel("10", "11", "2021"))
          )
          val expectedProperty = testProperty.copy(confirmed = true)

          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expectedProperty)

          When("POST business/overseas-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

          Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(yourIncomeSourcesURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expectedProperty, Some(1))
        }
      }

      "the user answered partial overseas property questions" should {
        "not save the property answers" in {
          enable(EnableTaskListRedesign)

          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
          When("POST business/overseas-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

          Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(yourIncomeSourcesURI)
          )

          verifyPost(subscriptionUri(OverseasProperty), count = Some(0))
        }
      }
    }
    "redirect to the agent task list page if the task list redesign feature switch is disabled" when {
      "the user answered all the overseas property questions" should {
        "save the property answers" in {
          val testProperty = OverseasPropertyModel(
            accountingMethod = Some(Cash),
            startDate = Some(DateModel("10", "11", "2021"))
          )
          val expectedProperty = testProperty.copy(confirmed = true)

          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testProperty))
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expectedProperty)

          When("POST business/overseas-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

          Then("Should return a SEE_OTHER with a redirect location of task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expectedProperty, Some(1))
        }
      }

      "the user answered partial overseas property questions" should {
        "not save the property answers" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash))))
          When("POST business/overseas-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers(Map(ITSASessionKeys.UTR -> testUtr))

          Then("Should return a SEE_OTHER with a redirect location of task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          verifyPost(subscriptionUri(OverseasProperty), count = Some(0))
        }
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "overseas property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "overseas property details cannot be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK,
          Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
