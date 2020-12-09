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

import config.featureswitch.FeatureSwitch.{PropertyNextTaxYear, ReleaseFour}
import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, MultipleIncomeSourcesSubscriptionAPIStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants.{checkYourAnswersURI => _, confirmationURI => _, incomeSourceURI => _, testNino => _, testUtr => _, _}
import helpers.IntegrationTestModels.{subscriptionData, testAccountingMethod, testBusinesses}
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import helpers.servicemocks.{EnrolmentStoreProxyStub, SubscriptionStub}
import models.{Accruals, Cash, DateModel, Next}
import models.common.{AccountingMethodPropertyModel, AccountingYearModel, IncomeSourceModel,
  OverseasAccountingMethodPropertyModel, OverseasPropertyStartDateModel, PropertyStartDateModel}
import models.common.business.BusinessSubscriptionDetailsModel
import play.api.http.Status.{OK, _}
import play.api.libs.json.Json
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}

class CheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(PropertyNextTaxYear)
    super.beforeEach()
  }

  "GET /check-your-answers" when {

    "the user has not answered the income sources question" should {
      "redirect the user to answer that question" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourceURI)
        )
      }
    }

    "the Subscription Details Connector returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.summary.title") + serviceNameGovUk)
        )

      }
    }
  }

  "POST /check-your-answers" should {

    "return an internal server error" when {
      "subscription was not successful" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
        SubscriptionStub.stubSuccessfulPostFailure(checkYourAnswersURI)

        When("I call POST /check-your-answers")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then(s"The result should have a status of $INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "release four is disabled" when {
      "property next tax year feature switch is disabled" when {
        "redirect to the confirmation page" when {
          "The whole subscription process was successful" in {

            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
            SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
            IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

            And("The wiremock stubs for auto enrolment")
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
            EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
            UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
            EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
            EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
            EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

            When("I call POST /check-your-answers")
            val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

            Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(confirmationURI)
            )

            val cookieMap = getSessionMap(res)
            cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

          }

          "subscription was successful but auto enrolment failed" when {
            "getting the group id the enrolment is allocated was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(NO_CONTENT)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "getting the users assigned to the enrolment was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(NO_CONTENT)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "getting the admin in a group was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "upserting the known facts was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "allocating the enrolment to a group was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "assigning all the users to the enrolment was not successful" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()
              SubscriptionStub.stubSuccessfulPostSubscription(checkYourAnswersURI)
              IncomeTaxSubscriptionConnectorStub.stubPostSubscriptionId()

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
          }
        }
      }
    }

    "release four is enabled" when {
      "property next tax year feature switch is disabled" when {
        "redirect to the confirmation page" should {
          "The whole subscription process was successful" when {
            "only self-employments selected and no other data" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "only self-employments selected and everything else answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "only UK property is answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
                selectedTaxYear = None,
                businessName = None,
                accountingMethod = None,
                propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                  propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "everything has been answered but the user has only got uk property selected" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
                  propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                  propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "only foreign property has been answered" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
                selectedTaxYear = None,
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010"))),
                  overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "everything has been answered but the user has only got foreign property selected" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010"))),
                  overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }

            "successfully send the correct details to the backend for a user with all income" in {
              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
                overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
                  propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
                  propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
                  overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010"))),
                  overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
                )
              )(NO_CONTENT)


              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

            }
          }
          "subscription was successful but auto enrolment failed" when {
            "getting the group id the enrolment is allocated was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(NO_CONTENT)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "getting the users assigned to the enrolment was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(NO_CONTENT)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "getting the admin in a group was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "upserting the known facts was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "allocating the enrolment to a group was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
            "assigning all the users to the enrolment was not successful" in {

              Given("I set the required feature switches")
              enable(ReleaseFour)

              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()
              IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
                incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
                selectedTaxYear = Some(AccountingYearModel(Next)),
                businessName = None,
                accountingMethod = None,
                propertyStartDate = None,
                propertyAccountingMethod = None,
                overseasPropertyAccountingMethod = None,
                overseasPropertyStartDate = None
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
                  propertyStartDate = None,
                  propertyAccountingMethod = None,
                  overseasPropertyStartDate = None,
                  overseasAccountingMethodProperty = None
                )
              )(NO_CONTENT)

              And("The wiremock stubs for auto enrolment")
              EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
              UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
              EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
              EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
              EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(NOT_FOUND)

              When("I call POST /check-your-answers")
              val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

              Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(confirmationURI)
              )

              val cookieMap = getSessionMap(res)
              cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
            }
          }
        }
      }

      "the property next tax year feature switch is enabled" when {
        "the signup of a user with next tax year property income is successful and creating of the enrolment is successful" in {

          Given("I set the required feature switches")
          enable(ReleaseFour)
          enable(PropertyNextTaxYear)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Next)),
            businessName = None,
            accountingMethod = None,
            propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
            propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
            overseasPropertyAccountingMethod = Some(OverseasAccountingMethodPropertyModel(Cash)),
            overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010")))
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
              propertyStartDate = Some(PropertyStartDateModel(DateModel("20", "03", "2000"))),
              propertyAccountingMethod = Some(AccountingMethodPropertyModel(Accruals)),
              overseasPropertyStartDate = Some(OverseasPropertyStartDateModel(DateModel("21", "03", "2010"))),
              overseasAccountingMethodProperty = Some(OverseasAccountingMethodPropertyModel(Cash))
            )
          )(NO_CONTENT)


          And("The wiremock stubs for auto enrolment")
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testUtr)(OK)
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, jsonResponseBody(principalUserIdKey, testCredentialId, testCredentialId2))
          UsersGroupsSearchStub.stubGetUsersForGroups(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
          EnrolmentStoreProxyStub.stubUpsertEnrolment(testSubscriptionID, testNino)(NO_CONTENT)
          EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testSubscriptionID, testGroupId, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId)(CREATED)
          EnrolmentStoreProxyStub.stubAssignEnrolment(testSubscriptionID, testCredentialId2)(CREATED)

          When("I call POST /check-your-answers")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val cookieMap = getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID
        }
      }
    }
  }

}
