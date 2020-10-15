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

package controllers.agent

import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.common.IncomeSourceModel
import org.jsoup.Jsoup
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class IncomeSourceControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(ForeignProperty)
    super.beforeEach()
  }


  "GET /report-quarterly/income-and-expenses/sign-up/client/income" when {
    "FS ForeignProperty is enabled" when {

      "the Subscription Details Connector returns all data" should {
        "show the income source page with business, UK property and foreign property options selected" in {
          enable(ForeignProperty)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()

          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading")),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
            checkboxSet(id = "UkProperty", selectedCheckbox = Some(messages("income_source.rentUkProperty"))),
            checkboxSet(id = "ForeignProperty", selectedCheckbox = Some(messages("income_source.foreignProperty")))
          )
        }
      }

      "the Subscription Details Connector returns no data" should {
        "show the income source page without an option selected" in {
          enable(ForeignProperty)
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()

          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading")),
            checkboxSet(id = "Business", selectedCheckbox = None),
            checkboxSet(id = "UkProperty", selectedCheckbox = None),
            checkboxSet(id = "ForeignProperty", selectedCheckbox = None)
          )
        }
      }
    }
    "FS ForeignProperty is disabled" when {
      "the Subscription Details Connector returns all data" should {
        "only show business and UK property options selected but does not display foreign property option" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

          When("GET /income is called")
          val res = IncomeTaxSubscriptionFrontend.income()

          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading")),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
            checkboxSet(id = "UkProperty", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
          )

          val checkboxes = Jsoup.parse(res.body).select(".multiple-choice")
          checkboxes.size() shouldBe 2

          val checkboxTextForeignProperty = Jsoup.parse(res.body).select(s"label[for=ForeignProperty]").text()
          checkboxTextForeignProperty shouldBe empty


        }
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/income" when {
    "FS ForeignProperty is disabled" when {
      "not in edit mode" when {
        "the user is self-employed, doesn't have uk and foreign property " in {
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of what year to sign up page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(whatYearToSignUp)
          )
        }
        "the user is self-employed and has uk property but doesn't have foreign property" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of business name page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessNameURI)
          )
        }

        "the user rents a uk property, is not self-employed and doesn't have foreign property" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyCommencementDateURI)
          )
        }
      }

      "in edit mode" when {
        "the user selects a different answer" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property accounting method")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyCommencementDateURI)
          )
        }
        "the user selects the same answer" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(userInput)))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answers")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
    }
  }
}
