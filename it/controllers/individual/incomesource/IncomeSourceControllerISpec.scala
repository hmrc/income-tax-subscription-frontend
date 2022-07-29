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

package controllers.individual.incomesource


import config.featureswitch.FeatureSwitch.ForeignProperty
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{testBusinessName, _}
import helpers.IntegrationTestModels._
import helpers.servicemocks.AuthStub
import models.common.business.{AccountingMethodModel, BusinessNameModel, SelfEmploymentData}
import models.common.{AccountingYearModel, IncomeSourceModel, OverseasPropertyModel, PropertyModel}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys._

class IncomeSourceControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(ForeignProperty)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {

    "the Subscription Details Connector returns all data" should {
      "show the income source page with the options selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "IncomeSource", selectedCheckbox = Some(messages("income_source.selfEmployed"))),
          checkboxSet(id = "IncomeSource-2", selectedCheckbox = Some(messages("income_source.rentUkProperty")))
        )
      }
    }

    "the Subscription Details Connector returns no data" should {
      "show the rent uk property page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("GET /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.incomeSource()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the rent uk property page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("income_source.title") + serviceNameGovUk),
          checkboxSet(id = "IncomeSource", selectedCheckbox = None),
          checkboxSet(id = "IncomeSource-2", selectedCheckbox = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-receive" when {
    "foreign property is disabled" when {
      "the user selected foreign property only" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of business name page")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "not in edit mode" when {
      "the user rents a uk property, is not self-employed and doesn't have foreign property" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }
      "the user rents a uk property and has foreign property but is not self-employed" in {
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of property commencement date page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyStartDateURI)
        )
      }
      "the user rents a foreign property, is not self-employed and doesn't have uk property" in {
        enable(ForeignProperty)
        val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.IncomeSource, userInput)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("POST /details/income-receive is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = false, Some(userInput))

        Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyStartDateURI)
        )
      }
    }

    "it is in edit mode" should {
      "the user selects self-employment and self-employment journey has not been completed before" when {
        s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)

          stubGetAllSubscriptionDetails(None, None, None, None, None, Some(testAccountingYearNext))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of self-employment frontend initialise")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
          )
        }
      }

      "the user selected UK property and UK property journey has not been completed before" when {
        s"redirect to ${controllers.individual.business.routes.PropertyStartDateController.show()}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, None, None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of property commencement date")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(propertyStartDateURI)
          )
        }
      }

      "the user selected overseas property and overseas property journey has not been completed before" when {
        s" redirect to ${controllers.individual.business.routes.OverseasPropertyStartDateController.show().url}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, None, None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyStartDateURI)
          )
        }
      }

      "the user select self-employment and self-employment journey has completed before" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show.url}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, None, Some(testAccountingYearCurrent))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }


      "the user select self-employment and UK property and both journeys have been completed before" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), Some(testFullPropertyModel), None, None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment and overseas property and both journeys have been completed before" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), None, Some(testFullOverseasPropertyModel), None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select self-employment, UK property and overseas property and all three journeys have been completed before" should {
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), None, Some(testAccountingMethod), Some(testFullPropertyModel), Some(testFullOverseasPropertyModel), None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select UK property and UK property journeys has been completed before" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, Some(testFullPropertyModel), Some(testFullOverseasPropertyModel), None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select overseas property and overseas property journeys has been completed before" should {
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.submit}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, None, Some(testFullOverseasPropertyModel), None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user select UK property and overseas property and both journeys have been completed before" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true))
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(None, None, None, Some(testFullPropertyModel), Some(testFullOverseasPropertyModel), None)

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

      "the user selects self-employment and no UK property or overseas property and self-employment journey has been completed before" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show}" in {
          val userInput: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
            subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              accountingMethod = Some(testAccountingMethod)
            )
          )

          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[IncomeSourceModel](subscriptionId, userInput)
          stubGetAllSubscriptionDetails(Some(testSummaryDataSelfEmploymentData), Some(testBusinessName), Some(testAccountingMethod), None, None, Some(testAccountingYearNext))

          When("POST /details/income-receive is called")
          val res = IncomeTaxSubscriptionFrontend.submitIncomeSource(inEditMode = true, Some(userInput))

          Then(s"Should return $SEE_OTHER with a redirect location of check your answer")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(checkYourAnswersURI)
          )
        }
      }

    }
  }


  def stubGetAllSubscriptionDetails(maybeBusinesses: Option[Seq[SelfEmploymentData]],
                                    maybeBusinessName: Option[BusinessNameModel],
                                    maybeBusinessAccountingMethod: Option[AccountingMethodModel],
                                    maybeProperty: Option[PropertyModel],
                                    maybeOverseasProperty: Option[OverseasPropertyModel],
                                    maybeSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    maybeBusinesses match {
      case None => stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
      case Some(businesses) => stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(businesses))
    }
    maybeBusinessName match {
      case None => stubGetSubscriptionDetails(BusinessName, NO_CONTENT)
      case Some(businessName) => stubGetSubscriptionDetails(BusinessName, OK, Json.toJson(businessName))
    }
    maybeBusinessAccountingMethod match {
      case None => stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)
      case Some(businessAccountingMethod) => stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(businessAccountingMethod))
    }
    maybeProperty match {
      case None => stubGetSubscriptionDetails(Property, NO_CONTENT)
      case Some(property) => stubGetSubscriptionDetails(Property, OK, Json.toJson(property))
    }
    maybeOverseasProperty match {
      case None => stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
      case Some(overseasProperty) => stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasProperty))
    }
    maybeSelectedTaxYear match {
      case None => stubGetSubscriptionDetails(SelectedTaxYear, NO_CONTENT)
      case Some(selectedTaxYear) => stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(selectedTaxYear))
    }
  }

}
