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

package helpers.servicemocks

import helpers.IntegrationTestConstants._
import models.usermatching.LockedOut
import play.api.http.Status

import java.time.OffsetDateTime


object UserLockoutStub extends WireMockMethods {

  def lockoutURI(userId: String): String = s"/income-tax-subscription/client-matching/lock/$userId"

  val testLock: LockedOut = LockedOut(testUserIdEncoded, OffsetDateTime.now())

  def stubLockAgent(userId: String): Unit =
    when(method = POST, uri = lockoutURI(userId))
      .thenReturn(Status.CREATED, testLock)

  def stubUserIsLocked(userId: String): Unit =
    when(method = GET, uri = lockoutURI(userId))
      .thenReturn(Status.OK, testLock)

  def stubUserIsNotLocked(userId: String): Unit =
    when(method = GET, uri = lockoutURI(userId))
      .thenReturn(Status.NOT_FOUND)

}
