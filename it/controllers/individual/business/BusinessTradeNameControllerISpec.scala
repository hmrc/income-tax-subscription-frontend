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

package controllers.individual.business

import config.featureswitch.FeatureSwitch
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.{stubGetSubscriptionDetails, stubSaveSelfEmploymentsDetails}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub._
import models.DateModel
import models.common.BusinessNameModel
import models.individual.business.{BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.BusinessesKey

class BusinessTradeNameControllerISpec extends ComponentSpecBase {

  FeatureSwitch.switches foreach enable

  val businessId: String = "testId"

  val testValidBusinessTradeName: String = "Plumbing"
  val testInvalidBusinessTradeName: String = "!()+{}?^~"
  val testValidBusinessTradeNameModel: BusinessTradeNameModel = BusinessTradeNameModel(testValidBusinessTradeName)
  val testInvalidBusinessTradeNameModel: BusinessTradeNameModel = BusinessTradeNameModel(testInvalidBusinessTradeName)

  val testBusiness: SelfEmploymentData = SelfEmploymentData(
    id = businessId,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1"))),
    businessName = Some(BusinessNameModel("testName")),
    businessTradeName = Some(testValidBusinessTradeNameModel)
  )

  "GET /report-quarterly/income-and-expenses/sign-up/self-employments/business/trade" when {

    "the user hasn't entered their business name" should {
      "redirect to the business name page" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness.copy(businessName = None, businessTradeName = None))))

        When("GET /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.getBusinessTradeName(businessId)

        Then("should return a SEE_OTHER to the business name page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessNameURI)
        )
      }
    }

    "the Connector receives no content" should {
      "return the page with no prepopulated fields" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness.copy(businessTradeName = None))))

        When("GET /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.getBusinessTradeName(businessId)

        Then("should return an OK with the BusinessTradeNamePage")
        res must have(
          httpStatus(OK),
          pageTitle("What is the trade of your business?"),
          textField("businessTradeName", "")
        )
      }
    }

    "Connector returns a previously filled in Business Trade Name" should {
      "show the current business trade name page with name values entered" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness)))

        When("GET /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.getBusinessTradeName(businessId)

        Then("should return an OK with the BusinessTradeNamePage")
        res must have(
          httpStatus(OK),
          pageTitle("What is the trade of your business?"),
          textField("businessTradeName", testValidBusinessTradeName)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/self-employments/business/trade" when {
    "not in edit mode" when {
      "the form data is valid and connector stores it successfully" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness.copy(businessTradeName = None))))
        stubSaveSelfEmploymentsDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness)))

        When("POST /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessTradeName(businessId, inEditMode = false, Some(testValidBusinessTradeNameModel))

        Then("Should return a SEE_OTHER")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAddressInitialiseUri(businessId))
        )
      }

      "the form data is valid but is a duplicate submission" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(
          testBusiness.copy(id = "idOne"),
          testBusiness.copy(id = "idTwo", businessTradeName = None)
        )))


        When("POST /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessTradeName("idTwo", inEditMode = false, Some(testValidBusinessTradeNameModel))

        Then("Should return a SEE_OTHER")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is the trade of your business?")
        )
      }

      "the form data is invalid and connector stores it unsuccessfully" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness.copy(businessTradeName = None))))

        When("POST /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessTradeName(businessId, inEditMode = false, Some(testInvalidBusinessTradeNameModel))

        Then("Should return a BAD_REQUEST and THE FORM With errors")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is the trade of your business?")
        )
      }

    }
    "in edit mode" when {
      "the form data is valid and connector stores it successfully" in {
        Given("I setup the Wiremock stubs")
        stubAuthSuccess()
        stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness.copy(businessTradeName = Some(BusinessTradeNameModel("test trade"))))))
        stubSaveSelfEmploymentsDetails(BusinessesKey, OK, Json.toJson(Seq(testBusiness)))

        When("POST /business/trade is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessTradeName(businessId, inEditMode = true, Some(testValidBusinessTradeNameModel))

        Then(s"Should return a $SEE_OTHER with a redirect location of check your answers")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessListCYAUri)
        )
      }
    }
  }
}
