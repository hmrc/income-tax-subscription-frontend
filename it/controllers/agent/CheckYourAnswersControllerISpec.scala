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

import connectors.agent.httpparsers.QueryUsersHttpParser.principalUserIdKey
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants.{testCredentialId, testCredentialId2, testGroupId, testUtr}
import helpers.IntegrationTestModels.{testAccountingMethod, testBusinesses, testEnrolmentKey}
import helpers.agent.IntegrationTestConstants._
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.EnrolmentStoreProxyStub.jsonResponseBody
import helpers.servicemocks.{EnrolmentStoreProxyStub, SubscriptionStub, TaxEnrolmentsStub}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}

class CheckYourAnswersControllerISpec extends ComponentSpecBase with SessionCookieCrumbler{

  "GET /check-your-answers" when {
    "the Subscription Details Connector returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.summary.title"))
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
