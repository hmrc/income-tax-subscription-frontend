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

package agent.services.mocks

import agent.audit.mocks.MockAuditingService
import agent.services.{UnexpectedFailure, _}
import agent.utils.TestConstants
import agent.utils.TestConstants.{testARN, testNino}
import agent.utils.TestModels.testClientDetails
import incometax.subscription.services.mocks.MockSubscriptionService
import usermatching.services.mocks.MockUserMatchingService

trait MockAgentQualificationService extends MockClientRelationshipService
  with MockUserMatchingService
  with MockSubscriptionService
  with MockAuditingService {

  object TestAgentQualificationService extends AgentQualificationService(
    mockUserMatchingService,
    mockClientRelationshipService,
    mockSubscriptionService,
    mockAuditingService
  )

  def setupOrchestrateAgentQualificationSuccess(arn: String = TestConstants.testARN,
                                                nino: String = TestConstants.testNino,
                                                isPreExistingRelationship: Boolean): Unit = {
    mockUserMatchSuccess(testClientDetails)
    setupMockGetSubscriptionNotFound(testNino)
    preExistingRelationship(testARN, testNino)(isPreExistingRelationship)
  }

  def setupOrchestrateAgentQualificationFailure(expectedResult: UnqualifiedAgent): Unit = {
    expectedResult match {
      case NoClientMatched => mockUserMatchNotFound(testClientDetails)
      case _ => mockUserMatchSuccess(testClientDetails)
    }

    expectedResult match {
      case ClientAlreadySubscribed => setupMockGetSubscriptionFound(testNino)
      case UnexpectedFailure => setupMockGetSubscriptionFailure(testNino)
      case _ => setupMockGetSubscriptionNotFound(testNino)
    }
  }

}
