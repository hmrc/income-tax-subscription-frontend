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

package agent.controllers.business

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels._
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import _root_.agent.services.CacheConstants
import agent.models.MatchTaxYearModel
import core.models.{No, Yes}
import play.api.http.Status._
import play.api.i18n.Messages

class MatchTaxYearControllerISpec extends ComponentSpecBase {

  s"GET ${routes.MatchTaxYearController.show().url}" when {
    "keystore has no data" should {
      "show the match tax year page with no radio option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When(s"GET ${routes.MatchTaxYearController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.match_tax_year.heading")),
          radioButtonSet(id = "matchToTaxYear", selectedRadioButton = None)
        )
      }
    }
    "keystore returns data" should {
      "show the match tax year page with the option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When(s"GET ${routes.MatchTaxYearController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.matchTaxYear()

        Then("Should return a OK with the accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.business.match_tax_year.heading")),
          radioButtonSet(id = "matchToTaxYear", selectedRadioButton = Some(Messages("base.yes")))
        )
      }
    }
  }

  s"POST ${routes.MatchTaxYearController.submit().url}" when {
    "in edit mode" when {
      "the answer the user has given has stayed the same" should {
        s"redirect to ${agent.controllers.routes.CheckYourAnswersController.show()}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)
          KeystoreStub.stubKeystoreData(keystoreData(matchTaxYear = Some(userInput)))

          When(s"POST ${routes.MatchTaxYearController.submit().url} is called")
          val res = IncomeTaxSubscriptionFrontend.submitMatchTaxYear(
            inEditMode = true,
            request = Some(userInput)
          )

          Then(s"Should redirect to ${agent.controllers.routes.CheckYourAnswersController.show().url}")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )

          KeystoreStub.verifyKeyStoreSave(CacheConstants.MatchTaxYear, userInput, Some(1))
        }
      }
      s"the answer the user has given has changed to '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in {
          val previousAnswer: MatchTaxYearModel = MatchTaxYearModel(No)
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)
          KeystoreStub.stubKeystoreData(keystoreData(matchTaxYear = Some(previousAnswer)))

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

          KeystoreStub.verifyKeyStoreSave(CacheConstants.MatchTaxYear, userInput, Some(1))
        }
      }
      s"the answer the user has given has changed to '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in {
          val previousAnswer: MatchTaxYearModel = MatchTaxYearModel(Yes)
          val userInput: MatchTaxYearModel = MatchTaxYearModel(No)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)
          KeystoreStub.stubKeystoreData(keystoreData(matchTaxYear = Some(previousAnswer)))

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

          KeystoreStub.verifyKeyStoreSave(CacheConstants.MatchTaxYear, userInput, Some(1))
        }
      }
    }
    "not in edit mode" when {
      s"the user answers '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(Yes)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)
          KeystoreStub.stubEmptyKeystore()

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

          KeystoreStub.verifyKeyStoreSave(CacheConstants.MatchTaxYear, userInput, Some(1))
        }
      }
      s"the user answers '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in {
          val userInput: MatchTaxYearModel = MatchTaxYearModel(No)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreSave(CacheConstants.MatchTaxYear, userInput)
          KeystoreStub.stubEmptyKeystore()

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

          KeystoreStub.verifyKeyStoreSave(CacheConstants.MatchTaxYear, userInput, Some(1))
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
