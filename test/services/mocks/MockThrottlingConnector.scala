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

package services.mocks

import connectors.ThrottlingConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.mockito.verification.VerificationMode
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import services.{Throttle, ThrottleId}
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockThrottlingConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {


  val mockThrottlingConnector: ThrottlingConnector = mock[ThrottlingConnector]
  val failFuzzyUrl = Math.random().toString

  def failClosed():Unit = permitRequestOnFailure(false)
  def failOpen():Unit = permitRequestOnFailure(true)
  private def permitRequestOnFailure(v:Boolean) = when(mockThrottle.failOpen).thenReturn(v)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockThrottlingConnector)
    when(mockThrottle.throttleId).thenReturn(mockThrottleId)
    when(mockThrottle.callOnFail).thenReturn(Call("test", failFuzzyUrl))
  }

  val mockThrottle = mock[Throttle]
  val mockThrottleId = mock[ThrottleId]
  when(mockThrottleId.name).thenReturn("TestThrottle")

  def throttled(): OngoingStubbing[Future[Boolean]] = set(Future.successful(false))

  def notThrottled(): OngoingStubbing[Future[Boolean]] = set(Future.successful(true))

  def throttleFail(): OngoingStubbing[Future[Boolean]] = set(Future.failed(new RuntimeException("testing throttle connector failure")))

  private def set(b: Future[Boolean]) = when(mockThrottlingConnector.getThrottleStatus(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(b)

  def verifyGetThrottleStatusCalls(mode: VerificationMode) = verify(mockThrottlingConnector, mode).getThrottleStatus(ArgumentMatchers.any())(ArgumentMatchers.any())
}
