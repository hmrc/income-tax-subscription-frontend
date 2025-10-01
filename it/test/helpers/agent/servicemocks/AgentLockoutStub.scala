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

package helpers.agent.servicemocks

import helpers.IntegrationTestConstants.testARN
import models.usermatching.LockedOut
import play.api.http.Status

import java.time.OffsetDateTime

object AgentLockoutStub extends WireMockMethods {

  def lockoutURI(arn: String): String = s"/income-tax-subscription/client-matching/lock/$arn"

  val testLock: LockedOut = LockedOut(testARN, OffsetDateTime.now())

  def stubLockAgent(arn: String): Unit =
    when(method = POST, uri = lockoutURI(arn))
      .thenReturn(Status.CREATED)

  def stubAgentIsLocked(arn: String): Unit =
    when(method = GET, uri = lockoutURI(arn))
      .thenReturn(Status.OK, testLock)

  def stubAgentIsNotLocked(arn: String): Unit =
    when(method = GET, uri = lockoutURI(arn))
      .thenReturn(Status.NOT_FOUND)

}
