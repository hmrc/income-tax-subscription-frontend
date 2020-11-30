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

package controllers.individual.subscription

import config.featureswitch.FeatureSwitch.{PropertyNextTaxYear, ReleaseFour}
import connectors.stubs._
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers._
import helpers.servicemocks._
import models.common._
import models.individual.business.BusinessSubscriptionDetailsModel
import models.{Accruals, Cash, DateModel, Next}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}

class CheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(PropertyNextTaxYear)
    super.beforeEach()
  }

  s"GET ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" when {
    "the release four feature switch is enabled" should {
      "show the check your answers page" in {
        Given("I set the required feature switches")
        enable(ReleaseFour)

        And("I setup the stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionGet()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("The check your answers page is displayed with an OK status")
        res should have(
          httpStatus(OK),
          pageTitle(messages("summary.title") + " - Report your income and expenses quarterly - GOV.UK")
        )
      }
    }
    "the release four feature switch is disabled" should {
      "show the check your answers page" in {
        Given("I setup the stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionGet()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("The check your answers page is displayed with an OK status")
        res should have(
          httpStatus(OK),
          pageTitle(messages("summary.title") + " - Report your income and expenses quarterly - GOV.UK")
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/check-your-answers" when {

    "the release four feature switch is enabled" when {
      "the property next tax year feature switch is disabled" when {
        "call the enrolment store successfully" should {
          "successfully send the correct details to the backend for a user with business income" when {
            "only the self employment has been answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyCommencementDate = None
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                  selfEmploymentsData = Some(testBusinesses),
                  accountingMethod = Some(testAccountingMethod.accountingMethod),
                  incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                  propertyCommencementDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyCommencementDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }
            "everything has been answered but the user has only got self employment selected" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
                  selfEmploymentsData = Some(testBusinesses),
                  accountingMethod = Some(testAccountingMethod.accountingMethod),
                  incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false),
                  propertyCommencementDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyCommencementDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }
          }

          "successfully send the correct details to the backend for a user with uk property income" when {
            "only uk property has been answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
                selectedTaxYear = None,
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = None,
                overseasPropertyCommencementDate = None
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                  selfEmploymentsData = None,
                  accountingMethod = None,
                  incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
                  propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                  propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                  overseasPropertyCommencementDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }
            "everything has been answered but the user has only got uk property selected" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                  selfEmploymentsData = None,
                  accountingMethod = None,
                  incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false),
                  propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                  propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                  overseasPropertyCommencementDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }
          }

          "successfully send the correct details to the backend for a user with foreign property income" when {
            "only foreign property has been answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
                selectedTaxYear = None,
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                  selfEmploymentsData = None,
                  accountingMethod = None,
                  incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
                  propertyCommencementDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                  overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }

            "everything has been answered but the user has only got foreign property selected" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              And("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
              ))

              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
              IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
              MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
                mtdbsa = testMtdId,
                request = BusinessSubscriptionDetailsModel(
                  nino = testNino,
                  accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                  selfEmploymentsData = None,
                  accountingMethod = None,
                  incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true),
                  propertyCommencementDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                  overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
                )
              )(NO_CONTENT)

              TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
              TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              When("POST /check-your-answers is called")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("Should return a SEE_OTHER with a redirect location of confirmation")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )
            }
          }

          "successfully send the correct details to the backend for a user with all income" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None,
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            When("POST /check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("Should return a SEE_OTHER with a redirect location of confirmation")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )
          }
        }

        "Known Facts call fails" should {
          "show the check your answers page" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None,
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, BAD_REQUEST)

            When("POST /check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("Should return an INTERNAL_SERVER_ERROR")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }
        }

        "enrolment failure occurs where not on whitelist" should {
          "show the check your answers page" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None,
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, FORBIDDEN)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

            When("POST /check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("Should return a INTERNAL SERVER ERROR status")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }
        }

        "enrolment failure occurs where missing details" should {
          "show the check your answers page" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None,
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

            When("POST /check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("Should return a INTERNAL SERVER ERROR status")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }
        }

        "enrolment failure occurs where auth success but access error with gateway token" should {
          "show the check your answers page" in {
            Given("I set the required feature switches")
            enable(ReleaseFour)

            And("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Next)),
              businessName = None,
              accountingMethod = None,
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
            ))

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))

            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
            MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
              mtdbsa = testMtdId,
              request = BusinessSubscriptionDetailsModel(
                nino = testNino,
                accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
                selfEmploymentsData = Some(testBusinesses),
                accountingMethod = Some(testAccountingMethod.accountingMethod),
                incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
                propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
                overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
              )
            )(NO_CONTENT)

            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, INTERNAL_SERVER_ERROR)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

            When("POST /check-your-answers is called")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("Should return a INTERNAL SERVER ERROR status")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }
        }

        "return an INTERNAL_SERVER_ERROR when a failure occurs when signing the user up" in {
          Given("I set the required feature switches")
          enable(ReleaseFour)

          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Next)),
            businessName = None,
            accountingMethod = None,
            propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
            propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
            overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
            overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
          ))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(INTERNAL_SERVER_ERROR)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }

        "return an INTERNAL_SERVER_ERROR when a failure occurs when creating income sources for the user" in {
          Given("I set the required feature switches")
          enable(ReleaseFour)

          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Next)),
            businessName = None,
            accountingMethod = None,
            propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
            propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
            overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
            overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
          ))

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
            mtdbsa = testMtdId,
            request = BusinessSubscriptionDetailsModel(
              nino = testNino,
              accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
              selfEmploymentsData = Some(testBusinesses),
              accountingMethod = Some(testAccountingMethod.accountingMethod),
              incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true),
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
              overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
            )
          )(INTERNAL_SERVER_ERROR)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
      "the property next tax year feature switch is enabled" when {
        "the signup of a user with next tax year property income is successful and creating of the enrolment is successful" in {
          Given("I set the required feature switches")
          enable(ReleaseFour)
          enable(PropertyNextTaxYear)

          And("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Next)),
            businessName = None,
            accountingMethod = None,
            propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
            propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
            overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
            overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010")))
          ))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, NO_CONTENT)

          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSignUp(testNino)(OK)
          MultipleIncomeSourcesSubscriptionAPIStub.stubPostSubscription(
            mtdbsa = testMtdId,
            request = BusinessSubscriptionDetailsModel(
              nino = testNino,
              accountingPeriod = AccountingPeriodUtil.getNextTaxYear,
              selfEmploymentsData = None,
              accountingMethod = None,
              incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true),
              propertyCommencementDate = Some(PropertyCommencementDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyCommencementDate = Some(OverseasPropertyCommencementDateModel(DateModel("21", "03", "2010"))),
              overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
            )
          )(NO_CONTENT)

          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return a SEE_OTHER with a redirect location of confirmation")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )
        }
      }
    }

    "the release four feature switch is disabled" when {
      "call the enrolment store successfully" should {
        "successfully send the correct details to the backend for a user with business and property income" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubIndividualSuccessfulSubscriptionPostWithBoth(checkYourAnswersURI)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()


          Then("Should return a SEE_OTHER with a redirect location of confirmation")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )
        }

        "successfully send the correct details to the backend for a user with property income" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionPropertyPost()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubSuccessfulSubscriptionPostWithProperty(checkYourAnswersURI)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, CREATED)
          IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()


          Then("Should return a SEE_OTHER with a redirect location of confirmation")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )
        }
      }

      "Known Facts call fails" should {
        "show the check your answers page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionFailure()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubSuccessfulSubscriptionPostWithProperty(checkYourAnswersURI)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, BAD_REQUEST)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "enrolment failure occurs where not on whitelist" should {
        "show the check your answers page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionFailure()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubSuccessfulSubscriptionPostWithBoth(checkYourAnswersURI)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, FORBIDDEN)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return a INTERNAL SERVER ERROR status")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "enrolment failure occurs where missing details" should {
        "show the check your answers page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionFailure()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubSuccessfulSubscriptionPostWithBoth(checkYourAnswersURI)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, BAD_REQUEST)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return a INTERNAL SERVER ERROR status")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "enrolment failure occurs where auth success but access error with gateway token" should {
        "show the check your answers page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionFailure()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessesKey, OK, Json.toJson(testBusinesses))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(BusinessAccountingMethod, OK, Json.toJson(testAccountingMethod))
          SubscriptionStub.stubSuccessfulSubscriptionPostWithBoth(checkYourAnswersURI)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testEnrolmentKey.asString, INTERNAL_SERVER_ERROR)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)

          When("POST /check-your-answers is called")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("Should return a INTERNAL SERVER ERROR status")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "return an INTERNAL_SERVER_ERROR when the backend service returns a NOT_FOUND" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionFailure()
        SubscriptionStub.stubCreateSubscriptionNotFound(checkYourAnswersURI)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return an INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

}
