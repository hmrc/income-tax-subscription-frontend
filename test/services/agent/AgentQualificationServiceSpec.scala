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

import models.audits.ClientMatchingAuditing.ClientMatchingAuditModel
import models.usermatching.UserDetailsModel
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.agent.mocks.MockAgentQualificationService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.agent.TestConstants._
import utilities.agent.TestModels._
import utilities.agent.{TestConstants, TestModels}

import scala.concurrent.Future

class AgentQualificationServiceSpec extends MockAgentQualificationService {

  def verifyClientMatchingSuccessAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = true))

  def verifyClientMatchingFailureAudit(): Unit =
    verifyAudit(ClientMatchingAuditModel(TestConstants.testARN, TestModels.testClientDetails, isSuccess = false))

  private val matchedClient = ApprovedAgent(testNino, Some(testUtr))
  private val unapprovedMatchedClient = UnApprovedAgent(testNino, Some(testUtr))

  def request(clientDetails: Option[UserDetailsModel] = None): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().buildRequest(clientDetails)

  "AgentQualificationService.matchClient" should {

    def call(clientDetails: UserDetailsModel, request: Request[AnyContent]): Future[TestAgentQualificationService.ReturnType] =
      TestAgentQualificationService.matchClient(clientDetails, testARN)(implicitly[HeaderCarrier], request)

    "return NoClientMatched if the client matching was unsuccessful" in {
      mockUserMatchNotFound(testClientDetails)

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      mockUserMatchSuccess(testClientDetails)

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, Some(testUtr)))

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

      response mustBe Left(ClientAlreadySubscribed(None))
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

      response mustBe Left(unapprovedMatchedClient)
    }

    "return ApprovedAgent if there is an existing relationship" in {
      preExistingRelationship(testARN, testNino)(isPreExistingRelationship = true)
      val response = await(call)

      response mustBe Right(matchedClient)
    }

  }

  "AgentQualificationService.orchestrateAgentQualification" should {

    def call(clientDetails: UserDetailsModel, request: Request[AnyContent]): Future[TestAgentQualificationService.ReturnType] =
      TestAgentQualificationService.orchestrateAgentQualification(clientDetails, testARN)(implicitly[HeaderCarrier], request)

    "return UnexpectedFailure if something went awry" in {
      preExistingRelationship(testARN, testClientDetails.nino)(true)

      setupOrchestrateAgentQualificationFailure(UnexpectedFailure)

      val response = await(call(testClientDetails, request(Some(testClientDetails))))

      response mustBe Left(UnexpectedFailure)
    }

    "return NoClientMatched if the client matching was unsuccessful" in {
      setupOrchestrateAgentQualificationFailure(NoClientMatched)

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Left(NoClientMatched)

      verifyClientMatchingFailureAudit()
    }

    "return ClientAlreadySubscribed if the client already has subscription" in {
      preExistingRelationship(testARN, testClientDetails.nino)(true)

      setupOrchestrateAgentQualificationFailure(ClientAlreadySubscribed(None))

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Left(ClientAlreadySubscribed(None))

      verifyClientMatchingSuccessAudit()
    }

    "return UnApprovedAgent if the agent does not have prior relationship with the client" in {

      setupOrchestrateAgentQualificationSuccess(isPreExistingRelationship = false)

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Left(UnApprovedAgent(testClientDetails.ninoInBackendFormat, Some(testUtr)))

      verifyClientMatchingSuccessAudit()
    }

    "return ApprovedAgent if the client matching was successful" in {
      setupOrchestrateAgentQualificationSuccess(isPreExistingRelationship = true)

      setupMockGetSubscriptionNotFound(testNino)

      val result = call(testClientDetails, request(Some(testClientDetails)))

      await(result) mustBe Right(ApprovedAgent(testClientDetails.ninoInBackendFormat, Some(testUtr)))

      verifyClientMatchingSuccessAudit()
    }
  }

}
