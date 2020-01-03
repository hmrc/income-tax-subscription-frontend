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

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.services.CacheConstants
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import incometax.subscription.models._
import play.api.http.Status._
import play.api.i18n.Messages

class IncomeSourceControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
    disable(AgentPropertyCashOrAccruals)
  }

  "GET /income" when {

    "keystore returns all data" should {
      "show the income source page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.income()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.income_source.title")),
          radioButtonSet(id = "incomeSource", selectedRadioButton = Some(Messages("agent.income_source.both")))
        )
      }
    }

    "keystore returns no data" should {
      "show the income source page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.income()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.income_source.title")),
          radioButtonSet(id = "incomeSource", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /income" when {

    "the eligibility pages feature switch is enabled" when {

      "not in edit mode" when {

        "the user selects the Both income source option" in {
          val userInput: IncomeSourceType = Both

          Given("I setup the wiremock stubs and enable feature switches")
          enable(EligibilityPagesFeature)
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

          When(s"POST ${routes.IncomeSourceController.submit()} is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of business name")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessNameURI)
          )
        }

        "the user selects the Business income source option" in {
          val userInput: IncomeSourceType = Business

          Given("I setup the wiremock stubs and enable feature switches")
          enable(EligibilityPagesFeature)
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

          When(s"POST ${routes.IncomeSourceController.submit()} is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of business name")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessNameURI)
          )
        }

        "the user selects the Property income source option" when {

          "the agent property cash or accruals feature switch is enabled" in {
            val userInput: IncomeSourceType = Property

            Given("I setup the wiremock stubs and enable feature switches")
            enable(EligibilityPagesFeature)
            enable(AgentPropertyCashOrAccruals)
            AuthStub.stubAuthSuccess()
            KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

            When(s"POST ${routes.IncomeSourceController.submit()} is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of match tax year")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(propertyAccountingMethodURI)
            )
          }

          "the agent property cash or accruals feature switch is disabled" in {
            val userInput: IncomeSourceType = Property

            Given("I setup the wiremock stubs and enable feature switches")
            enable(EligibilityPagesFeature)
            AuthStub.stubAuthSuccess()
            KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

            When(s"POST ${routes.IncomeSourceController.submit()} is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of match tax year")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(checkYourAnswersURI)
            )
          }

        }

      }

      "in edit mode" when {

        "the user changes their income to Business" in {
          val previousInput: IncomeSourceType = Both
          val userInput: IncomeSourceType = Business

          Given("I setup the wiremock stubs and enable feature switches")
          enable(EligibilityPagesFeature)
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(previousInput)))
          KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

          When(s"POST ${routes.IncomeSourceController.submit()} is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of business name")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessNameURI)
          )
        }

        "the user changes their income to Property" when {

          "the agent property cash or accruals feature switch is enabled" in {
            val previousInput: IncomeSourceType = Both
            val userInput: IncomeSourceType = Property

            Given("I setup the wiremock stubs and enable feature switches")
            enable(EligibilityPagesFeature)
            enable(AgentPropertyCashOrAccruals)
            AuthStub.stubAuthSuccess()
            KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(previousInput)))
            KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

            When(s"POST ${routes.IncomeSourceController.submit()} is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of match tax year")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(propertyAccountingMethodURI)
            )
          }

          "the agent property cash or accruals feature switch is disabled" in {
            val previousInput: IncomeSourceType = Both
            val userInput: IncomeSourceType = Property

            Given("I setup the wiremock stubs and enable feature switches")
            enable(EligibilityPagesFeature)
            AuthStub.stubAuthSuccess()
            KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(previousInput)))
            KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

            When(s"POST ${routes.IncomeSourceController.submit()} is called")
            val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

            Then(s"Should return $SEE_OTHER with a redirect location of match tax year")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(checkYourAnswersURI)
            )
          }

        }

      }

    }

    "not in edit mode" should {
      "select the Both income source radio button on the income source page" in {
        val userInput = Both

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the Business income source radio button on the income source page" in {
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "select the Property income source radio button on the income source page" in {
        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "select no income source option on the income source page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, "")

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {

      "simulate not changing income source from business when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Business
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing income source from property when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Property
        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing income source from business and property to both when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val userInput = Both
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing income source from business to property when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Business
        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing income source from property to both when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Property
        val userInput = Both

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }

      "simulate changing income source from business and property to business when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of business name")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
    }
  }
}