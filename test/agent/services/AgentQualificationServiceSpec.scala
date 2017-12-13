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
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.models.UserDetailsModel

class AgentQualificationServiceSpec extends MockAgentQualificationService {

  lazy val auditPath = agent.controllers.matching.routes.ConfirmClientController.submit().url

  def verifyClientMatchingSuccessAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = true), auditPath)

  def verifyClientMatchingFailureAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = false), auditPath)

  val matchedClient = ApprovedAgent(testNino, testUtr)

  def request(clientDetails: Option[UserDetailsModel] = None): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().buildRequest(clientDetails)

  "AgentQualificationService.matchClient" should {

    def call(request: Request[AnyContent]) = TestAgentQualificationService.matchClient(testARN)(implicitly[HeaderCarrier], request)

    "return NoClientDetails if there's no client details in session" in {
      val result = call(request())

      await(result) mustBe Left(NoClientDetails)
    }

    "return NoClientMatched if the client matching was unsuccessful" in {
      mockUserMatchNotFound(testClientDetails)

      val result = call(request(testClientDetails))

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      mockUserMatchSuccess(testClientDetails)

      val result = call(request(testClientDetails))

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

    def call(request: Request[AnyContent]) = TestAgentQualificationService.orchestrateAgentQualification(testARN)(implicitly[HeaderCarrier], request)

    "return UnexpectedFailure if something went awry" in {
      setupOrchestrateAgentQualificationFailure(UnexpectedFailure)

      val response = await(call(request(testClientDetails)))

      response mustBe Left(UnexpectedFailure)
    }

    "return NoClientDetails if there's no client details in session" in {
      setupOrchestrateAgentQualificationFailure(NoClientDetails)

      val result = call(request())

      await(result) mustBe Left(NoClientDetails)
    }

    "return NoClientMatched if the client matching was unsuccessful" in {
      setupOrchestrateAgentQualificationFailure(NoClientMatched)

      val result = call(request(testClientDetails))

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
    }

    "return ClientAlreadySubscribed if the client already has subscription" in {
      setupOrchestrateAgentQualificationFailure(ClientAlreadySubscribed)

      val result = call(request(testClientDetails))

      await(result) mustBe Left(ClientAlreadySubscribed)

      verifyClientMatchingSuccessAudit()
    }

    "return NoClientRelationship if the agent does not have prior relationship with the client" in {
      setupOrchestrateAgentQualificationFailure(NoClientRelationship)

      val result = call(request(testClientDetails))

      await(result) mustBe Left(NoClientRelationship)

      verifyClientMatchingSuccessAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      setupOrchestrateAgentQualificationSuccess()

      val result =  call(request(testClientDetails))

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, testUtr))

      verifyClientMatchingSuccessAudit()
    }
  }

}
