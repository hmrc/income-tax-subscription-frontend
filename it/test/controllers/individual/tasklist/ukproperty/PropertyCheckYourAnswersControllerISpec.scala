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

package controllers.individual.tasklist.ukproperty

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.subscriptionUri
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.agent.WiremockHelper.verifyPost
import helpers.servicemocks.AuthStub
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.Property

class PropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EnableTaskListRedesign)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/uk-property-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(accountingMethod = Some(Cash))))

      When("GET business/uk-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getPropertyCheckYourAnswers()

      Then("Should return OK with the property CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("business.check-your-answers.content.uk-property.title")} - Use software to send Income Tax updates - GOV.UK"
        )
      )
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("GET business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getPropertyCheckYourAnswers()

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/uk-property-check-your-answers" should {
    "redirect to the your income sources page if the task list redesign feature switch is enabled" when {
      "the user has answered all the questions for uk property" should {
        "redirect to the tasks list page and save property answers" in {
          enable(EnableTaskListRedesign)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021"))))
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
            PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true)
          )

          When("POST business/uk-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

          Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.yourIncomeSourcesURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveProperty(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true), Some(1))
        }
      }

      "the user has answered partial questions for uk property" should {
        "redirect to the tasks list page but not save property answers" in {
          enable(EnableTaskListRedesign)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(PropertyModel(accountingMethod = Some(Cash)))
          )

          When("POST business/uk-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

          Then("Should return a SEE_OTHER with a redirect location of the your income sources page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.yourIncomeSourcesURI)
          )

          verifyPost(subscriptionUri(Property), count = Some(0))
        }
      }
    }
    "redirect to the task list page if the task list redesign feature switch is disabled" when {
      "the user has answered all the questions for uk property" should {
        "redirect to the tasks list page and save property answers" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021"))))
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
            PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true)
          )

          When("POST business/uk-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

          Then("Should return a SEE_OTHER with a redirect location of task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveProperty(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true), Some(1))
        }
      }

      "the user has answered partial questions for uk property" should {
        "redirect to the tasks list page but not save property answers" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            Property,
            OK,
            Json.toJson(PropertyModel(accountingMethod = Some(Cash)))
          )

          When("POST business/uk-property-check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

          Then("Should return a SEE_OTHER with a redirect location of task list page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.taskListURI)
          )

          verifyPost(subscriptionUri(Property), count = Some(0))
        }
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the property details could not be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "the property details could not be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          Property,
          OK,
          Json.toJson(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021"))))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)

        When("POST business/uk-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
