/*
 * Copyright 2017 HM Revenue & Customs
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

package agent.services

import agent.audit.models.ClientMatchingAuditing.ClientMatchingAuditModel
import play.api.test.Helpers._
import agent.services.mocks.MockAgentQualificationService
import agent.utils.TestConstants._
import agent.utils.TestModels._
import agent.utils.{TestConstants, TestModels}

class AgentQualificationServiceSpec extends MockAgentQualificationService {

  lazy val auditPath = agent.controllers.matching.routes.ConfirmClientController.submit().url

  def verifyClientMatchingSuccessAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = true), auditPath)

  def verifyClientMatchingFailureAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = false), auditPath)

  val matchedClient = ApprovedAgent(testNino, testUtr)

  "AgentQualificationService.matchClient" should {

    def call = TestAgentQualificationService.matchClient(testARN)

    "return NoClientDetails if there's no client details in keystore" in {
      setupMockKeystore(fetchClientDetails = None)

      val result = call

      await(result) mustBe Left(NoClientDetails)
    }

    "return NoClientMatched if the client matching was unsuccessful" in {
      setupMockKeystore(fetchClientDetails = testClientDetails)
      mockUserMatchNotFound(testClientDetails)

      val result = call

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      setupMockKeystore(fetchClientDetails = testClientDetails)
      mockUserMatchSuccess(testClientDetails)

      val result = call

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, testUtr))

      verifyClientMatchingSuccessAudit()
    }
  }

  "AgentQualificationService.checkExistingSubscription" should {

    def call = TestAgentQualificationService.checkExistingSubscription(matchedClient)

    "return UnexpectedFailure if something went awry" in {
      setupMockGetSubscriptionFailure(testNino)

      val response = await(call)

      response mustBe Left(UnexpectedFailure)
    }

    "return ClientAlreadySubscribed if the client already has a subscription" in {
      setupMockGetSubscriptionFound(testNino)

      val response = await(call)

      response mustBe Left(ClientAlreadySubscribed)
    }

    "return ApprovedAgent if the client does not have a subscription" in {
      setupMockGetSubscriptionNotFound(testNino)

      val response = await(call)

      response mustBe Right(matchedClient)
    }
  }

  "AgentQualificationService.checkClientRelationship" should {

    def call = TestAgentQualificationService.checkClientRelationship(testARN, matchedClient)

    "return UnexpectedFailure if something went awry" in {
      preExistingRelationshipFailure(testARN, testNino)(new Exception())
      val response = await(call)

      response mustBe Left(UnexpectedFailure)
    }

    "return NoClientRelationship if there are no existing relationships" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = false)
      val response = await(call)

      response mustBe Left(NoClientRelationship)
    }

    "return ApprovedAgent if there is an existing relationship" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = true)
      val response = await(call)

      response mustBe Right(matchedClient)
    }

  }

  "AgentQualificationService.orchestrateAgentQualification" should {

    def call = TestAgentQualificationService.orchestrateAgentQualification(testARN)

    "return UnexpectedFailure if something went awry" in {
      setupOrchestrateAgentQualificationFailure(UnexpectedFailure)

      val response = await(call)

      response mustBe Left(UnexpectedFailure)
      verifyKeystore(fetchClientDetails = 1)
    }

    "return NoClientDetails if there's no client details in keystore" in {
      setupOrchestrateAgentQualificationFailure(NoClientDetails)

      val result = call

      await(result) mustBe Left(NoClientDetails)
      verifyKeystore(fetchClientDetails = 1)
    }

    "return NoClientMatched if the client matching was unsuccessful" in {
      setupOrchestrateAgentQualificationFailure(NoClientMatched)

      val result = call

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
      verifyKeystore(fetchClientDetails = 1)
    }

    "return ClientAlreadySubscribed if the client already has subscription" in {
      setupOrchestrateAgentQualificationFailure(ClientAlreadySubscribed)

      val result = call

      await(result) mustBe Left(ClientAlreadySubscribed)

      verifyClientMatchingSuccessAudit()
      verifyKeystore(fetchClientDetails = 1)
    }

    "return NoClientRelationship if the agent does not have prior relationship with the client" in {
      setupOrchestrateAgentQualificationFailure(NoClientRelationship)

      val result = call

      await(result) mustBe Left(NoClientRelationship)

      verifyClientMatchingSuccessAudit()
      verifyKeystore(fetchClientDetails = 1)
    }

    "return ApprovedAgent if the client matching was successful" in {
      setupOrchestrateAgentQualificationSuccess()

      val result = call

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, testUtr))

      verifyClientMatchingSuccessAudit()
      verifyKeystore(fetchClientDetails = 1)
    }
  }

}
