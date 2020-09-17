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

package controllers.agent.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels._
import helpers.agent.servicemocks.AuthStub
import models.common.IncomeSourceModel
import models.individual.business.MatchTaxYearModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys

class MatchTaxYearControllerISpec extends ComponentSpecBase {

  s"GET ${routes.MatchTaxYearController.show().url}" when {
    "the Subscription Details Connector has no data" should {
      "show the match tax year page with no radio option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When(s"GET ${routes.MatchTaxYearController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the match tax year page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.match_tax_year.heading")),
          radioButtonSet(id = "matchToTaxYear", selectedRadioButton = None)
        )
      }
    }
    "Data returned from mongo" should {
      "show the match tax year page with the option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When(s"GET ${routes.MatchTaxYearController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the match tax year page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.business.match_tax_year.heading")),
          radioButtonSet(id = "matchToTaxYear", selectedRadioButton = Some(messages("base.yes")))
        )
      }
    }
  }

  s"POST ${routes.MatchTaxYearController.submit().url}" when {
    "in edit mode" when {
      "the answer the user has given has stayed the same" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)

          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson((IncomeSourceModel(true, true, false))),
            SubscriptionDataKeys.MatchTaxYear -> Json.toJson(MatchTaxYearModel(Yes))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.MatchTaxYear, userInput)
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(matchTaxYear = Some(userInput),
            incomeSource = Some(IncomeSourceModel(true, true, false))))

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = true,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${controllers.agent.routes.CheckYourAnswersController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.MatchTaxYear, expectedCacheMap, Some(1))
        }
      }
      s"the answer the user has given has changed to '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in {
          val previousAnswer: MatchTaxYearModel = MatchTaxYearModel(No)
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)

          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson(IncomeSourceModel(true, true, false)),
            SubscriptionDataKeys.MatchTaxYear -> Json.toJson(MatchTaxYearModel(Yes))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.MatchTaxYear, userInput)
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(matchTaxYear = Some(previousAnswer),
            incomeSource = Some(IncomeSourceModel(true, true, false))))

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = true,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${routes.BusinessAccountingMethodController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessAccountingMethodURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.MatchTaxYear, expectedCacheMap, Some(1))
        }
      }
      s"the answer the user has given has changed to '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in {
          val previousAnswer: MatchTaxYearModel = MatchTaxYearModel(Yes)
          val userInput: MatchTaxYearModel = MatchTaxYearModel(No)

          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson(IncomeSourceModel(true, true, false)),
            SubscriptionDataKeys.MatchTaxYear -> Json.toJson(MatchTaxYearModel(No))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.MatchTaxYear, userInput)
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(matchTaxYear = Some(previousAnswer),
            incomeSource = Some(IncomeSourceModel(true, true, false))))

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = true,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${routes.BusinessAccountingPeriodDateController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingPeriodDatesURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.MatchTaxYear, expectedCacheMap, Some(1))
        }
      }
    }
    "not in edit mode" when {
      s"the user answers '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)
          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson(IncomeSourceModel(true, true, false)),
            SubscriptionDataKeys.MatchTaxYear -> Json.toJson(MatchTaxYearModel(Yes))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.MatchTaxYear, userInput)

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = false,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${routes.BusinessAccountingMethodController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(businessAccountingMethodURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.MatchTaxYear, expectedCacheMap, Some(1))
        }
      }
      s"the user answers '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(No)

          val expectedCacheMap = CacheMap("", Map(
            SubscriptionDataKeys.IncomeSource -> Json.toJson(IncomeSourceModel(true, true, false)),
            SubscriptionDataKeys.MatchTaxYear -> Json.toJson(MatchTaxYearModel(No))))

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(IncomeSourceModel(true, true, false))))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.MatchTaxYear, userInput)

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = false,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${routes.BusinessAccountingPeriodDateController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingPeriodDatesURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifySubscriptionSave(SubscriptionDataKeys.MatchTaxYear, expectedCacheMap, Some(1))
        }
      }
      "the user does not select an answer" should {
        s"return a $BAD_REQUEST" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = false,
            request = None
          )

          res should have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
        }
      }
    }
  }

}
