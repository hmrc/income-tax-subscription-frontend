/*
 * Copyright 2023 HM Revenue & Customs
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

import config.MockConfig
import config.featureswitch.FeatureSwitch
import models.ConnectorError
import models.common.subscription.SubscriptionSuccess
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.test.Helpers._
import services.agent.mocks.{MockAgentSPSConnector, MockClientRelationshipService}
import services.mocks.{MockAutoEnrolmentService, MockSubscriptionService}
import uk.gov.hmrc.http.InternalServerException
import utilities.TestModels.testAccountingPeriodThisYear
import utilities.agent.TestConstants._

import scala.concurrent.Future

class SubscriptionOrchestrationServiceSpec extends MockSubscriptionService
  with MockAutoEnrolmentService
  with MockClientRelationshipService
  with MockAgentSPSConnector {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(FeatureSwitch.CheckClientRelationship)
    disable(FeatureSwitch.CheckMultiAgentRelationship)
  }

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockAutoEnrolmentService,
    mockClientRelationshipService,
    mockAgentSpsConnector
  )(MockConfig)

  val testTaxYear: String = testAccountingPeriodThisYear.toLongTaxYear

  "createSubscriptionFromTaskList" should {
    def res: Future[Either[ConnectorError, Option[SubscriptionSuccess]]] = {
      TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)
    }

    "return a success" when {

      "all services succeed" in {
        mockSignUpSuccess(testNino, testTaxYear)
        preExistingMTDRelationship(testARN, testNino)(isPreExistingMTDRelationship = true)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(Right(AutoEnrolmentService.EnrolmentAssigned))
        mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)

        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
        verifyCheckPreExistingMTDRelationship(testARN, testNino)
        verifyCheckMTDSuppAgentRelationship(testARN, testNino, 0)
      }

      "there is no pre existing agent-client relationship" in {
        mockSignUpSuccess(testNino, testTaxYear)
        preExistingMTDRelationship(testARN, testNino)(isPreExistingMTDRelationship = false)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
        mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(Right(AutoEnrolmentService.EnrolmentAssigned))
        mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)

        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)

        await(res) mustBe testSubscriptionSuccess
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
        verifyCheckPreExistingMTDRelationship(testARN, testNino)
        verifyCheckMTDSuppAgentRelationship(testARN, testNino, 0)
      }

      "the sign up indicated the customer was already signed up" in {
        mockAlreadySignedUp(testNino, testTaxYear)

        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)

        await(res) mustBe Right(None)
        verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 0)
      }

      "the CheckMultiAgentRelationship feature switch is on" when {
        "there is a multi-agent-client relationship" in {
          enable(FeatureSwitch.CheckMultiAgentRelationship)
          mockSignUpSuccess(testNino, testTaxYear)
          preExistingMTDRelationship(testARN, testNino)(isPreExistingMTDRelationship = false)
          suppAgentRelationship(testARN, testNino)(isMTDSuppAgentRelationship = true)
          mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
          mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(Right(AutoEnrolmentService.EnrolmentAssigned))
          mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)

          val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)

          await(res) mustBe testSubscriptionSuccess
          verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
          verifyCheckPreExistingMTDRelationship(testARN, testNino)
          verifyCheckMTDSuppAgentRelationship(testARN, testNino)
        }
        "there is no supporting-agent client relationship" in {
          enable(FeatureSwitch.CheckMultiAgentRelationship)
          mockSignUpSuccess(testNino, testTaxYear)
          preExistingMTDRelationship(testARN, testNino)(isPreExistingMTDRelationship = false)
          suppAgentRelationship(testARN, testNino)(isMTDSuppAgentRelationship = false)
          mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
          mockAutoClaimEnrolment(testUtr, testNino, testMTDID)(Right(AutoEnrolmentService.EnrolmentAssigned))
          mockAgentSpsConnectorSuccess(testARN, testUtr, testNino, testMTDID)

          val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)

          await(res) mustBe testSubscriptionSuccess
          verifyAgentSpsConnector(testARN, testUtr, testNino, testMTDID, 1)
          verifyCheckPreExistingMTDRelationship(testARN, testNino)
          verifyCheckMTDSuppAgentRelationship(testARN, testNino)
        }
      }
    }

    "return a failure" when {
      "sign up income sources request fail and returns an error" in {
        mockSignUpIncomeSourcesFailure(testNino, testTaxYear)

        whenReady(res)(_ mustBe testSignUpIncomeSourcesFailure)
      }

      "create income sources from task list request fail and returns an error" in {
        mockSignUpSuccess(testNino, testTaxYear)
        mockCreateIncomeSourcesFromTaskListFailure(testMTDID, testCreateIncomeSourcesThisYear)

        whenReady(res)(_ mustBe testCreateSubscriptionFromTaskListFailure)
      }

      "check agent-client relationship fails" in {
        enable(FeatureSwitch.CheckMultiAgentRelationship)

        mockSignUpSuccess(testNino, testTaxYear)
        mockCreateIncomeSourcesFromTaskListSuccess(testMTDID, testCreateIncomeSourcesThisYear)
        preExistingMTDRelationshipFailure(testARN, testNino)(failure = testException)

        val res = TestSubscriptionOrchestrationService.createSubscriptionFromTaskList(testARN, testUtr, testCreateIncomeSourcesThisYear)
        intercept[Exception](await(res))

        verifyCheckPreExistingMTDRelationship(testARN, testNino, 0)
        verifyCheckPreExistingMTDRelationship(testARN, testNino, 0)
      }
    }
  }
}
