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
import helpers.IntegrationTestModels.subscriptionData
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import models.common.IncomeSourceModel
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
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
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("agent.income_source.selfEmployed"))),
            checkboxSet(id = "UKProperty", selectedCheckbox = Some(messages("agent.income_source.rentUkProperty"))),
            checkboxSet(id = "ForeignProperty", selectedCheckbox = Some(messages("agent.income_source.foreignProperty")))
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
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = None),
            checkboxSet(id = "UKProperty", selectedCheckbox = None),
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
          val serviceNameGovUK = " - Report your income and expenses quarterly - GOV.UK"
          Then("Should return a OK with the income source page")
          res should have(
            httpStatus(OK),
            pageTitle(messages("agent.income_source.heading") + serviceNameGovUK),
            checkboxSet(id = "Business", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
            checkboxSet(id = "UKProperty", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
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

    "FS ForeignProperty & ReleaseFour are disabled" when {
      "not in edit mode" when {

        "the user is self-employed only" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails("testId", userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of what year to sign up page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(whatYearToSignUp)
          )
        }

        "the user is self-employed and has a UK property " in {
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

        "the user has only a UK property " in {
          val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyAccountingMethodURI)
          )
        }
      }

      "in edit mode" when {
        "the user is self-employed only" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
            Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))

          When("POST /income is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }
      "the user is self-employed and UK property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is UK property only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

  "FS ForeignProperty is enabled but ReleaseFour is disabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of what tax year to sign up page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(whatYearToSignUpURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ForeignProperty)
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

      "the user has a UK property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property accounting method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $INTERNAL_SERVER_ERROR with an internal server error page")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "the user has a UK property and has a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of UK property accounting method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyAccountingMethodURI)
        )
      }

      "the user is self-employed and has a UK property and a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER  with a redirect location of business name page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
    }

    "in edit mode" when {
      "the user is self-employed only" in {

        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and UK property" in {

        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is UK property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has foreign property only" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and has a UK property and a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has a UK property and a foreign property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

  "FS ReleaseFour is enabled but ForeignProperty is disabled" when {
    "not in edit mode" when {

      "the user is self-employed only" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)


        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of what tax year to sign up page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(whatYearToSignUpURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ReleaseFour)
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

      "the user has a UK property only" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyCommencementDateURI)
        )
      }
    }

    "in edit mode" when {
      "the user is self-employed only" in {

        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and UK property" in {

        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has UK property only" in {
        enable(ReleaseFour)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }

  "Both FS ReleaseFour and ForeignProperty are enabled" when {
    "not in edit mode" when {
      "the user is self-employed only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of what tax year to sign up page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(whatYearToSignUpURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
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

      "the user is self-employed and has a foreign property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
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

      "the user is self-employed and has a UK property and a foreign property " in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
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

      "the user has a UK property and a foreign property " in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyCommencementDateURI)
        )
      }

      "the user has a UK property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of UK property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyCommencementDateURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $INTERNAL_SERVER_ERROR with an internal server error")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "in edit mode" when {

      "the user is self-employed only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and has a UK property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and has a foreign property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user is self-employed and has a UK property and a foreign property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(true, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has a UK property and a foreign property" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has a UK property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, true, false)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "the user has a foreign property only" in {
        enable(ReleaseFour)
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(false, false, true)
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails("subscriptionId", OK,
          Json.toJson(Some(CacheMap(SubscriptionDataKeys.IncomeSource, subscriptionData(incomeSource = Some(userInput))))))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of check your answer page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}
