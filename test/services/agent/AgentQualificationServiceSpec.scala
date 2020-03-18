/*
 * Copyright 2020 HM Revenue & Customs
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

import models.audits.ClientMatchingAuditing.ClientMatchingAuditModel
import agent.utils.TestConstants._
import agent.utils.TestModels._
import agent.utils.{TestConstants, TestModels}
import models.usermatching.UserDetailsModel
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.agent.mocks.MockAgentQualificationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AgentQualificationServiceSpec extends MockAgentQualificationService {

  def verifyClientMatchingSuccessAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = true))

  def verifyClientMatchingFailureAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = false))

  val matchedClient = ApprovedAgent(testNino, testUtr)
  val unapprovedMatchedClient = UnApprovedAgent(testNino, testUtr)

  def request(clientDetails: Option[UserDetailsModel] = None): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().buildRequest(clientDetails)

  "AgentQualificationService.matchClient" should {

    def call(request: Request[AnyContent]): Future[TestAgentQualificationService.ReturnType] =
      TestAgentQualificationService.matchClient(testARN)(implicitly[HeaderCarrier], request)

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

    def call: Future[TestAgentQualificationService.ReturnType] = TestAgentQualificationService.checkExistingSubscription(matchedClient)

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

    def call: Future[TestAgentQualificationService.ReturnType] =
      TestAgentQualificationService.checkClientRelationship(testARN, matchedClient)

    "return UnexpectedFailure if something went awry" in {
      preExistingRelationshipFailure(testARN, testNino)(new Exception())
      val response = await(call)

      response mustBe Left(UnexpectedFailure)
    }
//
    "return UnApprovedAgent if there are no existing relationships" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = false)
      val response = await(call)

      response mustBe Right(unapprovedMatchedClient)
    }

    "return ApprovedAgent if there is an existing relationship" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = true)
      val response = await(call)

      response mustBe Right(matchedClient)
    }

  }

  "AgentQualificationService.orchestrateAgentQualification" should {

    def call(request: Request[AnyContent]): Future[TestAgentQualificationService.ReturnType] =
      TestAgentQualificationService.orchestrateAgentQualification(testARN)(implicitly[HeaderCarrier], request)

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

    "return UnApprovedAgent if the agent does not have prior relationship with the client" in {

      setupOrchestrateAgentQualificationSuccess(isPreExistingRelationship = false)

      val result = call(request(testClientDetails))

      await(result) mustBe Right(UnApprovedAgent(testClientDetails.ninoInBackendFormat, testUtr))

      verifyClientMatchingSuccessAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      setupOrchestrateAgentQualificationSuccess(isPreExistingRelationship = true)

      val result =  call(request(testClientDetails))

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, testUtr))

      verifyClientMatchingSuccessAudit()
    }
  }

}
