/*
 * Copyright 2022 HM Revenue & Customs
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

package services.agent

import config.featureswitch.FeatureSwitch.SPSEnabled
import models.ConnectorError
import models.common.subscription.{CreateIncomeSourcesSuccess, SubscriptionSuccess}
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.test.Helpers._
import services.agent.mocks.MockAgentSPSConnector
import services.mocks.{MockAutoEnrolmentService, MockSubscriptionService}
import utilities.SubscriptionDataUtil.{disable, enable}
import utilities.TestModels.testAgentSummaryData
import utilities.agent.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends MockSubscriptionService with MockAutoEnrolmentService with MockAgentSPSConnector {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockAutoEnrolmentService,
    mockAgentSpsConnector
  )

  override def beforeEach(): Unit = {
    disable(SPSEnabled)
    super.beforeEach()
  }

  "createSubscription when release four is disabled" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testUtr, testAgentSummaryData)
    }

    "return a success" when {
      "all services succeed" in {
        mockCreateSubscriptionSuccess(testNino, testAgentSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }
      "the auto enrolment service returns a failure response" in {
        mockCreateSubscriptionSuccess(testNino, testAgentSummaryData, testARN)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.NoUsersFound)

        await(res) mustBe testSubscriptionSuccess
      }
    }

    "return a failure" when {
      "create subscription returns an error" in {
        mockCreateSubscriptionFailure(testNino, testAgentSummaryData, testARN)

        await(res) mustBe testSubscriptionFailure
      }

    }
  }

  "createSubscription when release four is enabled" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscription(testARN, testNino, testUtr, testAgentSummaryData, isReleaseFourEnabled = true)
    }

    "return a success" when {
      "all services succeed" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }

      "SpsIsEnabled and all services succeed" in {
        enable(SPSEnabled)
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData)
        mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)
        val res = TestSubscriptionOrchestrationService.createSubscription(
          testARN, testNino, testUtr, testAgentSummaryData, isReleaseFourEnabled = true)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
      }

      "SpsIsEnabled and all services except the SPSconnector succeed" in {
        enable(SPSEnabled)
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData)
        mockAgentSpsConnectorFailure(testARN, testUtr, testNino, testMTDID)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)
        val res = TestSubscriptionOrchestrationService.createSubscription(
          testARN, testNino, testUtr, testAgentSummaryData, isReleaseFourEnabled = true)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
      }
    }

    "return a failure" when {
      "create income sources returns an error when sign up income sources request fail" in {
        mockSignUpIncomeSourcesFailure(testNino)

        await(res) mustBe testSignUpIncomeSourcesFailure

      }

      "create income sources returns an error when create income sources request fail" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFailure(testNino, testMTDID, testAgentSummaryData)
        await(res) mustBe testCreateIncomeSourcesFailure
      }

      "the auto enrolment service returns a failure response" in {
        enable(SPSEnabled)

        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesSuccess(testNino, testMTDID, testAgentSummaryData)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.NoUsersFound)

        await(res) mustBe Right(SubscriptionSuccess(testMTDID))
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 0)

      }


    }
  }

  "createSubscriptionFromTaskList when saveAndRetrieve is enabled" should {

    def res: Future[Either[ConnectorError, SubscriptionSuccess]] = {
      TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testNino, testUtr, testCreateIncomeSources)
    }

    "return a success" when {
      "all services succeed" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)

        await(res) mustBe testSubscriptionSuccess
      }

      "SpsIsEnabled and all services succeed" in {
        enable(SPSEnabled)
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSources)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(AutoEnrolmentService.EnrolmentAssigned)
        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testNino, testUtr, testCreateIncomeSources)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
      }
    }

    "return a failure" when {
      "sign up income sources request fail and returns an error" in {
        mockSignUpIncomeSourcesFailure(testNino)

        whenReady(res)(_ mustBe testSignUpIncomeSourcesFailure)
      }

      "create income sources from task list request fail and returns an error" in {
        mockSignUpIncomeSourcesSuccess(testNino)
        mockCreateIncomeSourcesFromTaskListFailure(testMTDID, testCreateIncomeSources)

        whenReady(res)(_ mustBe testCreateSubscriptionFromTaskListFailure)
      }

    }
  }
}
